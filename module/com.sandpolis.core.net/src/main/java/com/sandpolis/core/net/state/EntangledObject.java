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

import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STObject;
import com.sandpolis.core.instance.state.oid.Oid;
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
public abstract class EntangledObject<T extends Message> extends AbstractSTObject {

	protected StreamSink<T> sink;
	protected StreamSource<T> source;

	public StreamSource<T> getSource() {
		return source;
	}

	public StreamSink<T> getSink() {
		return sink;
	}

	@Subscribe
	void handle(STAttribute.ChangeEvent<?> event) {
		getSource().submit((T) eventToProto(event.attribute, event.newValue));
	}

	private Message eventToProto(STAttribute<?> attribute, Object value) {
		int[] components = attribute.oid().relativize(((STObject<?>) container()).oid().parent()).value();

		MessageOrBuilder current = null;

		for (int i = components.length - 1; i >= 0; i--) {
			System.out.println(components[i]);
			switch (Oid.type(components[i])) {
			case Oid.TYPE_ATTRIBUTE:
				// TODO use value parameter
				current = attribute.snapshot();
				break;
			case Oid.TYPE_DOCUMENT:
				switch (Oid.type(components[i + 1])) {
				case Oid.TYPE_ATTRIBUTE:
					current = ProtoDocument.newBuilder().putAttribute(components[i + 1], (ProtoAttribute) current);
					break;
				case Oid.TYPE_DOCUMENT:
					current = ProtoDocument.newBuilder().putDocument(components[i + 1],
							((ProtoDocument.Builder) current).build());
					break;
				case Oid.TYPE_COLLECTION:
					current = ProtoDocument.newBuilder().putCollection(components[i + 1],
							((ProtoCollection.Builder) current).build());
					break;
				default:
					throw new RuntimeException();
				}

				break;
			case Oid.TYPE_COLLECTION:
				current = ProtoCollection.newBuilder().putDocument(components[i + 1],
						((ProtoDocument.Builder) current).build());
				break;
			default:
				throw new RuntimeException();
			}
		}

		if (current instanceof Builder) {
			return ((Builder) current).build();
		} else {
			return (Message) current;
		}
	}

	protected abstract AbstractSTObject container();

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

	protected void startSink(STSyncStruct config, Class<T> messageType) {
		sink = new StreamSink<>() {

			@Override
			public void onNext(T item) {
				((STObject<T>) container()).merge(item);
			};
		};

		StreamStore.add(new InboundStreamAdapter<>(config.streamId, config.connection, messageType), getSink());
	}
}
