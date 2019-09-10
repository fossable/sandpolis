/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.stream.store;

import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.net.store.connection.Events.SockLostEvent;
import com.sandpolis.core.proto.net.MCStream.EV_StreamData;
import com.sandpolis.core.stream.store.StreamStore.StreamStoreConfig;

/**
 * There are four "banks" that each serve a specific purpose.
 *
 * <pre>
 * IN     OUT    SRC    SINK
 * [A]    [B]    [B]    [A]
 * [ ]    [A]    [ ]    [ ]
 * [ ]    [A]    [ ]    [ ]
 * [ ]    [ ]    [ ]    [ ]
 * [ ]    [ ]    [ ]    [ ]
 * </pre>
 *
 * @author cilki
 * @since 5.0.2
 */
public final class StreamStore extends StoreBase<StreamStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(StreamStore.class);

	public StreamStore() {
		super(log);
	}

	/**
	 * The SOURCE bank.
	 */
	static List<StreamSource<?>> source;

	/**
	 * The SINK bank.
	 */
	static List<StreamSink<?>> sink;

	/**
	 * The INBOUND bank.
	 */
	static List<InboundStreamAdapter<?>> inbound;

	/**
	 * The OUTBOUND bank.
	 */
	static List<OutboundStreamAdapter<?>> outbound;

	public static void streamData(EV_StreamData data) {
		for (var adapter : inbound) {
			if (adapter.getStreamID() == data.getId()) {
				adapter.submit(data);
				break;
			}
		}
	}

	public static void stop(int streamID) {
		iterateInbound(it -> {
			while (it.hasNext()) {
				var adapter = it.next();
				if (adapter.getStreamID() == streamID) {
					it.remove();
					adapter.close();
					break;
				}
			}
		});
		iterateOutbound(it -> {
			while (it.hasNext()) {
				var adapter = it.next();
				if (adapter.getStreamID() == streamID) {
					it.remove();
					// adapter.close();
					break;
				}
			}
		});
		iterateSource(it -> {
			while (it.hasNext()) {
				var source = it.next();
				if (source.getStreamID() == streamID) {
					it.remove();
					source.close();
					break;
				}
			}
		});
		iterateSink(it -> {
			while (it.hasNext()) {
				var sink = it.next();
				if (sink.getStreamID() == streamID) {
					it.remove();
					sink.close();
					break;
				}
			}
		});

		// TODO find dependent streams to also close
	}

	private static void iterateSource(Consumer<Iterator<StreamSource<?>>> mutator) {
		synchronized (source) {
			synchronized (sink) {
				synchronized (inbound) {
					synchronized (outbound) {
						mutator.accept(source.iterator());
					}
				}
			}
		}
	}

	private static void iterateSink(Consumer<Iterator<StreamSink<?>>> mutator) {
		synchronized (source) {
			synchronized (sink) {
				synchronized (inbound) {
					synchronized (outbound) {
						mutator.accept(sink.iterator());
					}
				}
			}
		}
	}

	private static void iterateInbound(Consumer<Iterator<InboundStreamAdapter<?>>> mutator) {
		synchronized (source) {
			synchronized (sink) {
				synchronized (inbound) {
					synchronized (outbound) {
						mutator.accept(inbound.iterator());
					}
				}
			}
		}
	}

	private static void iterateOutbound(Consumer<Iterator<OutboundStreamAdapter<?>>> mutator) {
		synchronized (source) {
			synchronized (sink) {
				synchronized (inbound) {
					synchronized (outbound) {
						mutator.accept(outbound.iterator());
					}
				}
			}
		}
	}

	@Subscribe
	private void onSockLost(SockLostEvent event) {
		iterateInbound(it -> {
			while (it.hasNext()) {
				var adapter = it.next();
				if (adapter.getSock().equals(event.get())) {
					it.remove();
					adapter.close();
					break;
				}
			}
		});
		iterateOutbound(it -> {
			while (it.hasNext()) {
				var adapter = it.next();
				if (adapter.getSock().equals(event.get())) {
					it.remove();
					// adapter.close();
					break;
				}
			}
		});
	}

	@Override
	public StreamStore init(Consumer<StreamStoreConfig> configurator) {
		var config = new StreamStoreConfig();
		configurator.accept(config);

		ConnectionStore.register(this);

		return (StreamStore) super.init(null);
	}

	public final class StreamStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			source = new LinkedList<>();
			sink = new LinkedList<>();
			inbound = new LinkedList<>();
			outbound = new LinkedList<>();
		}
	}

	public static final StreamStore StreamStore = new StreamStore();
}
