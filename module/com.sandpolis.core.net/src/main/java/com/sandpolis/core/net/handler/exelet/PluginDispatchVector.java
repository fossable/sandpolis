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
package com.sandpolis.core.net.handler.exelet;

import java.util.Objects;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ProtoUtil;

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
	protected Message unwrapPayload(MSG.Message msg) throws InvalidProtocolBufferException {
		return ProtoUtil.getPayload(msg.getPlugin().unpack(messageType));
	}

	@Override
	protected MSG.Message wrapPayload(MSG.Message msg, MessageOrBuilder payload) {
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
	public boolean accept(MSG.Message msg) throws Exception {
		Message payload = msg.getPlugin().unpack(messageType);

		var payloadCase = payload.getClass().getMethod("getPayloadCase").invoke(payload);
		int payloadTag = (int) payloadCase.getClass().getMethod("getNumber").invoke(payloadCase);

		return dispatch(msg, payloadTag);
	}
}
