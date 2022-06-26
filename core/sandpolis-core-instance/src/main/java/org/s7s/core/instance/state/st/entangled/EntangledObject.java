//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.st.entangled;

import static org.s7s.core.instance.stream.StreamStore.StreamStore;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import org.s7s.core.protocol.Stream.EV_STStreamData;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.AbstractSTObject;
import org.s7s.core.instance.state.st.STAttribute;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.st.STObject;
import org.s7s.core.instance.state.STCmd.STSyncStruct;
import org.s7s.core.instance.stream.InboundStreamAdapter;
import org.s7s.core.instance.stream.OutboundStreamAdapter;
import org.s7s.core.instance.stream.Stream;
import org.s7s.core.instance.stream.StreamSink;
import org.s7s.core.instance.stream.StreamSource;

/**
 * An {@link EntangledObject} is synchronized with a remote object on another
 * instance.
 *
 * <p>
 * It uses the {@link Stream} API to efficiently send real-time updates to the
 * remote object.
 *
 * @param <T> The type of the object's protobuf representation
 * @since 7.0.0
 */
public abstract class EntangledObject extends AbstractSTObject {

	public EntangledObject(STDocument parent, String id) {
		super(parent, id);
	}

	private static final Logger log = LoggerFactory.getLogger(EntangledObject.class);

	protected StreamSink<EV_STStreamData> sink;

	protected StreamSource<EV_STStreamData> source;

	/**
	 * A future object that is notified when the entanglement becomes "inactive".
	 */
	private final CompletableFuture<Void> inactive = new CompletableFuture<>();

	protected STObject container;

	public StreamSink<EV_STStreamData> getSink() {
		return sink;
	}

	public StreamSource<EV_STStreamData> getSource() {
		return source;
	}

	public boolean isActive() {
		return !inactive.isDone();
	}

	public CompletableFuture<Void> getInactiveFuture() {
		return inactive;
	}

	protected void startSink(STSyncStruct config) {
		sink = new StreamSink<>() {

			@Override
			public void onNext(EV_STStreamData item) {
				container.merge(item);
			};

			@Override
			public void close() {
				inactive.complete(null);
			};
		};

		StreamStore.add(new InboundStreamAdapter<>(config.streamId, config.connection, EV_STStreamData.class), sink);
	}

	protected void startSource(STSyncStruct config) {
		source = new StreamSource<>() {

			@Override
			public void start() {
				container.addListener(EntangledObject.this);
			}

			@Override
			public void close() {
				container.removeListener(EntangledObject.this);
				inactive.complete(null);
			}

			@Override
			public String getStreamKey() {
				return container.oid().toString();
			}
		};

		StreamStore.add(source, new OutboundStreamAdapter<>(config.streamId, config.connection));

		source.start();

		// Send initial state after starting the stream. This ensures no updates are
		// missed, but can cause them to be received in the wrong order. Let the sink
		// reorder them according to timestamp values.
		container.snapshot(snapshot_config -> {
			snapshot_config.oid = container.oid();
		}).forEach(source::submit);

		// Close the stream now if requested
		if (!config.permanent) {
			log.debug("Requested stream was not permanent");
			close();
		}
	}

	public void close() {
		if (source != null) {
			StreamStore.stop(source.getStreamID());
		}
		if (sink != null) {
			StreamStore.stop(sink.getStreamID());
		}
	}

	@Override
	public Oid oid() {
		return container.oid();
	}

	@Override
	public STDocument parent() {
		return container.parent();
	}

	@Override
	public void addListener(Object listener) {
		container.addListener(listener);
	}

	@Override
	public void removeListener(Object listener) {
		container.removeListener(listener);
	}

	@Subscribe
	void handle(STAttribute.ChangeEvent event) {
		event.attribute().snapshot(config -> {
			config.oid = container.oid();
		}).forEach(source::submit);
	}

	@Subscribe
	void handle(STDocument.DocumentAddedEvent event) {
		event.newDocument().snapshot(config -> {
			config.oid = container.oid();
		}).forEach(source::submit);
	}

	@Subscribe
	void handle(STDocument.DocumentRemovedEvent event) {
		source.submit(
				EV_STStreamData.newBuilder().setRemoved(true).setOid(Arrays.stream(event.oldDocument().oid().path())
						.skip(container.oid().path().length).collect(Collectors.joining("/"))).build());
	}

	@Override
	public void replaceParent(STDocument parent) {
		container.replaceParent(parent);
	}
}
