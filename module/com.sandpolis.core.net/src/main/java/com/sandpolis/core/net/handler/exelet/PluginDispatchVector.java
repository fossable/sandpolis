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
package com.sandpolis.core.net.handler.exelet;

import java.util.Objects;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.util.ProtoUtil;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.util.Result.Outcome;

public class PluginDispatchVector extends DispatchVector {

	private final String pluginId;

	private final Class<? extends Message> messageType;

	/**
	 * Build a plugin {@link DispatchVector} with the given message class.
	 *
	 * @param sock     The sock
	 * @param pluginId The plugin ID
	 * @param msgCls   The plugin message class
	 */
	public PluginDispatchVector(Sock sock, String pluginId, Class<? extends Message> msgCls) {
		super(sock);
		this.pluginId = Objects.requireNonNull(pluginId);
		this.messageType = Objects.requireNonNull(msgCls);
	}

	private Message.Builder newMessage() {
		try {
			return (Message.Builder) messageType.getMethod("newBuilder").invoke(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Message unwrapPayload(MSG msg) throws InvalidProtocolBufferException {
		return ProtoUtil.getPayload(msg.getPlugin().unpack(messageType));
	}

	@Override
	protected MSG wrapPayload(MSG msg, MessageOrBuilder payload) {
		// Build the payload if not already built
		if (payload instanceof Builder)
			payload = ((Builder) payload).build();

		// Handle special case for Outcome
		if (payload instanceof Outcome)
			return ProtoUtil.rs(msg).setRsOutcome((Outcome) payload).build();

		var m = newMessage();

		FieldDescriptor field = m.getDescriptorForType()
				.findFieldByName(ProtoUtil.convertMessageClassToFieldName(payload.getClass()));

		return ProtoUtil.rs(msg).setPlugin(Any.pack(m.setField(field, payload).build(), pluginId)).build();
	}

	@Override
	public boolean accept(MSG msg) throws Exception {
		Message payload = msg.getPlugin().unpack(messageType);

		var payloadCase = payload.getClass().getMethod("getPayloadCase").invoke(payload);
		int payloadTag = (int) payloadCase.getClass().getMethod("getNumber").invoke(payloadCase);

		return dispatch(msg, payloadTag);
	}
}
