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

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.oid.OidBase;
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
	void handle(STAttribute.ChangeEvent<T> event) {
		getSource().submit((T) eventToProto(event.attribute, event.newValue));
	}

	private <E> Message eventToProto(STAttribute<E> attribute, E value) {
		int[] components = attribute.oid().value();

		MessageOrBuilder current = null;

		for (int i = components.length - 1; i >= 0; i--) {
			switch (components[i] % 10) {
			case OidBase.SUFFIX_ATTRIBUTE:
				// TODO use value parameter
				current = attribute.snapshot();
				break;
			case OidBase.SUFFIX_DOCUMENT:
				switch (components[i + 1] % 10) {
				case OidBase.SUFFIX_ATTRIBUTE:
					current = ProtoDocument.newBuilder().putAttribute(components[i], (ProtoAttribute) current);
					break;
				case OidBase.SUFFIX_DOCUMENT:
					current = ProtoDocument.newBuilder().putDocument(components[i],
							((ProtoDocument.Builder) current).build());
					break;
				case OidBase.SUFFIX_COLLECTION:
					current = ProtoDocument.newBuilder().putCollection(components[i],
							((ProtoCollection.Builder) current).build());
					break;
				}
				break;
			case OidBase.SUFFIX_COLLECTION:
				current = ProtoCollection.newBuilder().putDocument(components[i],
						((ProtoDocument.Builder) current).build());
				break;
			}
		}

		if (current instanceof Builder) {
			return ((Builder) current).build();
		} else {
			return (Message) current;
		}
	}
}
