//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.MessageLite;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.MetadataStore;
import org.s7s.core.instance.store.StoreBase;
import org.s7s.core.instance.store.StoreMetadata;
import org.s7s.core.instance.connection.ConnectionStore.SockLostEvent;
import org.s7s.core.instance.stream.StreamEndpoint.StreamPublisher;
import org.s7s.core.instance.stream.StreamEndpoint.StreamSubscriber;
import org.s7s.core.instance.stream.StreamStore.StreamStoreConfig;
import org.s7s.core.instance.stream.StreamStore.StreamStoreMetadata;

/**
 * The {@link StreamStore} contains four "banks" of stream endpoints that each
 * serve a specific purpose.
 * <ul>
 * <li><b>INBOUND:</b> Receives and unwraps events from the network and routes
 * them into the OUTBOUND or SINK banks.</li>
 * <li><b>OUTBOUND:</b> Receives events from the INBOUND or SOURCE banks and
 * routes them into the network.</li>
 * <li><b>SOURCE:</b> Streams in this bank produce events and routes them into
 * the OUTBOUND or SINK banks.</li>
 * <li><b>SINK:</b> Streams in this bank consume events from the INBOUND or
 * SOURCE banks.</li>
 * </ul>
 *
 * <p>
 * This architecture elegantly supports multicast streams. For example, an
 * endpoint in the INBOUND bank can easily and efficiently route incoming
 * messages from one connection to multiple outgoing connections and vice-versa.
 *
 * <p>
 * The graphic below illustrates a multicast stream A and a unicast stream B.
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
 * @since 5.0.2
 */
public final class StreamStore extends StoreBase
		implements ConfigurableStore<StreamStoreConfig>, MetadataStore<StreamStoreMetadata> {

	private static final Logger log = LoggerFactory.getLogger(StreamStore.class);

	public StreamStore() {
		super(log);
	}

	private static record StreamConnection<E extends MessageLite> (StreamPublisher<E> publisher,
			StreamSubscriber<E> subscriber) {
	}

	private final StreamStoreMetadata metadata = new StreamStoreMetadata();

	private List<StreamConnection> connections;

	public synchronized <E extends MessageLite> void add(InboundStreamAdapter<E> in, OutboundStreamAdapter<E> out) {
		checkArgument(!in.isSubscribed(out));
		log.debug("Connecting inbound stream {} to outbound stream {}", in, out);
		in.subscribe(out);

		connections.add(new StreamConnection<>(in, out));
	}

	public synchronized <E extends MessageLite> void add(InboundStreamAdapter<E> in, StreamSink<E> sink) {
		checkArgument(!in.isSubscribed(sink));
		log.debug("Connecting inbound stream {} to sink stream {}", in, sink);
		in.subscribe(sink);

		connections.add(new StreamConnection<>(in, sink));
	}

	public synchronized <E extends MessageLite> void add(StreamSource<E> source, OutboundStreamAdapter<E> out) {
		checkArgument(!source.isSubscribed(out));
		log.debug("Connecting source stream {} to outbound stream {}", source, out);
		source.subscribe(out);

		connections.add(new StreamConnection<>(source, out));
	}

	public synchronized <E extends MessageLite> void add(StreamSource<E> source, StreamSink<E> sink) {
		checkArgument(!source.isSubscribed(sink));
		log.debug("Connecting source stream {} to sink stream {}", source, sink);
		source.subscribe(sink);

		connections.add(new StreamConnection<>(source, sink));
	}

	public synchronized void streamData(int id, MessageLite data) {
		inboundBank().filter(adapter -> adapter.getStreamID() == id).findFirst().ifPresent(adapter -> {
			adapter.submit(data);
		});
	}

	public Stream<StreamSource> sourceBank() {
		return connections.stream().map(s -> s.publisher()).filter(StreamSource.class::isInstance)
				.map(StreamSource.class::cast);
	}

	public Stream<StreamSink> sinkBank() {
		return connections.stream().map(s -> s.subscriber()).filter(StreamSink.class::isInstance)
				.map(StreamSink.class::cast);
	}

	public Stream<InboundStreamAdapter> inboundBank() {
		return connections.stream().map(s -> s.publisher()).filter(InboundStreamAdapter.class::isInstance)
				.map(InboundStreamAdapter.class::cast);
	}

	public Stream<OutboundStreamAdapter> outboundBank() {
		return connections.stream().map(s -> s.subscriber()).filter(OutboundStreamAdapter.class::isInstance)
				.map(OutboundStreamAdapter.class::cast);
	}

	public Stream<StreamPublisher<?>> publishers() {
		return connections.stream().map(s -> s.publisher()).map(StreamPublisher.class::cast);
	}

	public Stream<StreamSubscriber<?>> subscribers() {
		return connections.stream().map(s -> s.subscriber()).map(StreamSubscriber.class::cast);
	}

	/**
	 * Immediately halt a stream by its ID. Other streams may also be stopped as a
	 * result of stopping the target stream.
	 *
	 * @param id The stream ID
	 */
	public synchronized void stop(int id) {

		connections.removeIf(connection -> {
			if (connection.publisher().getStreamID() == id) {
				log.trace("Stopping connected stream endpoints: {}, {}", connection.publisher().getStreamID(),
						connection.subscriber().getStreamID());

				connection.publisher().close();
				connection.subscriber().close();
				return true;
			} else if (connection.subscriber().getStreamID() == id) {

				// Only close the publisher if this subscriber is the last remaining subscriber
				if (publishers().filter(sp -> sp.getStreamID() == connection.publisher().getStreamID()).count() == 1) {
					connection.publisher().close();
				}

				connection.subscriber().close();
				return true;
			}
			return false;
		});
	}

	@Subscribe
	private synchronized void onSockLost(SockLostEvent event) {
		Stream.concat(
				inboundBank().filter(in -> in.getSock().equals(event.connection())).map(StreamEndpoint::getStreamID),
				outboundBank().filter(in -> in.getSock().equals(event.connection())).map(StreamEndpoint::getStreamID))
				.collect(Collectors.toList()) //
				.forEach(StreamStore::stop);
	}

	@Override
	public StreamStoreMetadata getMetadata() {
		return metadata;
	}

	@Override
	public void init(Consumer<StreamStoreConfig> configurator) {
		var config = new StreamStoreConfig(configurator);

		connections = new ArrayList<>();

		ConnectionStore.register(this);
	}

	public final class StreamStoreMetadata implements StoreMetadata {

		@Override
		public int getInitCount() {
			return 1;
		}

		public long sourceSize() {
			return sourceBank().count();
		}

		public long sinkSize() {
			return sinkBank().count();
		}

		public long inboundSize() {
			return inboundBank().count();
		}

		public long outboundSize() {
			return outboundBank().count();
		}
	}

	public static final class StreamStoreConfig {
		private StreamStoreConfig(Consumer<StreamStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	/**
	 * The global context {@link StreamStore}.
	 */
	public static final StreamStore StreamStore = new StreamStore();
}
