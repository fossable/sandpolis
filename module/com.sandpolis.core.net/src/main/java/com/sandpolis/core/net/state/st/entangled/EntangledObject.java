//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.state.st.entangled;

import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.MessageLite;
import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.AbstractSTObject;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.st.STObject;
import com.sandpolis.core.net.state.STCmd.STSyncStruct;
import com.sandpolis.core.net.stream.InboundStreamAdapter;
import com.sandpolis.core.net.stream.OutboundStreamAdapter;
import com.sandpolis.core.net.stream.Stream;
import com.sandpolis.core.net.stream.StreamSink;
import com.sandpolis.core.net.stream.StreamSource;

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
public abstract class EntangledObject<T extends MessageLite> extends AbstractSTObject<T> {

	public EntangledObject(STDocument parent, Oid oid) {
		super(parent, oid);
	}

	private static final Logger log = LoggerFactory.getLogger(EntangledObject.class);

	protected StreamSink<T> sink;

	protected StreamSource<T> source;

	public StreamSink<T> getSink() {
		return sink;
	}

	public StreamSource<T> getSource() {
		return source;
	}

	protected abstract STObject<T> container();

	protected void startSink(STSyncStruct config, Class<T> messageType) {
		sink = new StreamSink<>() {

			@Override
			public void onNext(T item) {
				if (log.isTraceEnabled()) {
					log.trace("Merging snapshot: {}", item);
				}
				((STObject<T>) container()).merge(item);
			};
		};

		StreamStore.add(new InboundStreamAdapter<>(config.streamId, config.connection, messageType), getSink());
	}

	protected void startSource(STSyncStruct config) {
		source = new StreamSource<>() {

			@Override
			public void start() {
				container().addListener(EntangledObject.this);
			}

			@Override
			public void stop() {
				container().removeListener(EntangledObject.this);
			}
		};

		StreamStore.add(getSource(), new OutboundStreamAdapter<>(config.streamId, config.connection));
		getSource().start();
	}

	@Subscribe
	void handle(STAttribute.ChangeEvent<?> event) {
		var snapshot = ProtoAttribute.newBuilder(event.attribute.snapshot());
		snapshot.setPath(event.attribute.oid().toString());

		getSource().submit((T) snapshot.build());
	}

	@Subscribe
	void handle(STDocument.DocumentAddedEvent event) {
		var snapshot = ProtoDocument.newBuilder(event.document.snapshot());
		snapshot.setPath(event.document.oid().toString());

		getSource().submit((T) snapshot.build());
	}

	@Subscribe
	void handle(STDocument.DocumentRemovedEvent event) {
		getSource().submit(
				(T) ProtoDocument.newBuilder().setPath(event.document.oid().toString()).setRemoval(true).build());
	}
}
