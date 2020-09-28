//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.net.state;

import static com.sandpolis.core.instance.state.oid.OidUtil.*;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STCollection;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.STObject;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.OidUtil;
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
public abstract class EntangledObject<T extends Message> extends AbstractSTObject<T> {

	protected StreamSink<T> sink;

	protected StreamSource<T> source;

	public StreamSink<T> getSink() {
		return sink;
	}

	public StreamSource<T> getSource() {
		return source;
	}

	private Message eventToProto(Oid oid, Message snapshot) {
		long[] components = oid.relativize(container().oid().parent()).value();

		for (int i = components.length - 2; i >= 0; i--) {
			switch (OidUtil.getOidType(components[i])) {
			case OTYPE_DOCUMENT:
				switch (OidUtil.getOidType(components[i + 1])) {
				case OTYPE_ATTRIBUTE:
					snapshot = ProtoDocument.newBuilder().addAttribute((ProtoAttribute) snapshot).build();
					break;
				case OTYPE_DOCUMENT:
					snapshot = ProtoDocument.newBuilder().addDocument(((ProtoDocument) snapshot)).build();
					break;
				case OTYPE_COLLECTION:
					snapshot = ProtoDocument.newBuilder().addCollection(((ProtoCollection) snapshot)).build();
					break;
				default:
					throw new RuntimeException();
				}

				break;
			case OTYPE_COLLECTION:
				snapshot = ProtoCollection.newBuilder().addDocument(((ProtoDocument) snapshot)).build();
				break;
			default:
				throw new RuntimeException("Invalid OID component: " + components[i]);
			}
		}

		return snapshot;
	}

	protected abstract STObject<T> container();

	protected void startSink(STSyncStruct config, Class<T> messageType) {
		sink = new StreamSink<>() {

			@Override
			public void onNext(T item) {
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
		getSource().submit((T) eventToProto(event.attribute.oid(), event.attribute.snapshot()));
	}

	@Subscribe
	void handle(STCollection.DocumentAddedEvent event) {
		getSource().submit((T) eventToProto(event.newDocument.oid(), event.newDocument.snapshot()));
	}

	@Subscribe
	void handle(STCollection.DocumentRemovedEvent event) {
		getSource()
				.submit((T) eventToProto(event.oldDocument.oid(), ProtoDocument.newBuilder().setRemoval(true).build()));
	}

	@Subscribe
	void handle(STDocument.CollectionAddedEvent event) {
		getSource().submit((T) eventToProto(event.newCollection.oid(), event.newCollection.snapshot()));
	}

	@Subscribe
	void handle(STDocument.CollectionRemovedEvent event) {
		getSource().submit(
				(T) eventToProto(event.oldCollection.oid(), ProtoCollection.newBuilder().setRemoval(true).build()));
	}
}
