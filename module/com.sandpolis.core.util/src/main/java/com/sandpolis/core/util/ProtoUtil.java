/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.proto.net.MCStream.EV_StreamData;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * Utilities for simplifying common operations related to protocol buffers.
 * Using a static import is a convenient way to use these methods.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class ProtoUtil {

	/**
	 * The {@code oneof} descriptor that will contain the message response.
	 */
	public static final OneofDescriptor MSG_ONEOF = MSG.Message.getDescriptor().getOneofs().get(0);

	public static final OneofDescriptor STREAM_ONEOF = EV_StreamData.getDescriptor().getOneofs().get(0);

	/**
	 * Begin an action that should be completed with {@link #success} or
	 * {@link #failure}.
	 * 
	 * @return A new outcome builder
	 */
	public static Outcome.Builder begin() {
		return Outcome.newBuilder().setTime(System.currentTimeMillis());
	}

	/**
	 * Begin an action that should be completed with {@link #success} or
	 * {@link #failure}.
	 * 
	 * @param action The action description
	 * @return A new outcome builder
	 */
	public static Outcome.Builder begin(String action) {
		return begin().setAction(action);
	}

	/**
	 * Complete an action with a successful result.
	 * 
	 * @param outcome The outcome builder to complete
	 * @return The final outcome
	 */
	public static Outcome success(Outcome.Builder outcome) {
		return outcome.setResult(true).setTime(System.currentTimeMillis() - outcome.getTime()).build();
	}

	/**
	 * Complete an action with a successful result.
	 * 
	 * @param outcome The outcome builder to complete
	 * @param comment The action comment
	 * @return The final outcome
	 */
	public static Outcome success(Outcome.Builder outcome, String comment) {
		return outcome.setResult(true).setTime(System.currentTimeMillis() - outcome.getTime()).setComment(comment)
				.build();
	}

	/**
	 * Complete an action with an unsuccessful result.
	 * 
	 * @param outcome The outcome builder to complete
	 * @param comment The action comment
	 * @return The final outcome
	 */
	public static Outcome failure(Outcome.Builder outcome, String comment) {
		return failure(outcome.setComment(comment));
	}

	/**
	 * Complete an action with an unsuccessful result.
	 * 
	 * @param outcome The outcome builder to complete
	 * @param cause   The exception that caused the failure
	 * @return The final outcome
	 */
	public static Outcome failure(Outcome.Builder outcome, Throwable cause) {
		if (cause == null)
			throw new IllegalArgumentException();

		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			cause.printStackTrace(pw);

			return failure(outcome.setException(sw.toString()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Complete an action with an unsuccessful result.
	 * 
	 * @param outcome The outcome builder to complete
	 * @return The final outcome
	 */
	public static Outcome failure(Outcome.Builder outcome) {
		return outcome.setResult(false).clearTime().build();
	}

	/**
	 * Complete an action with an unspecified result.
	 * 
	 * @param outcome The outcome builder to complete
	 * @return The final outcome
	 */
	public static Outcome complete(Outcome.Builder outcome) {
		return outcome.setTime(System.currentTimeMillis() - outcome.getTime()).build();
	}

	/**
	 * Create a new request message.
	 * 
	 * @return A new request builder
	 */
	public static Message.Builder rq() {
		return Message.newBuilder().setId(IDUtil.msg());
	}

	/**
	 * Create a new response message.
	 * 
	 * @param id The sequence ID
	 * @return A new response builder
	 */
	public static Message.Builder rs(int id) {
		return Message.newBuilder().setId(id);
	}

	/**
	 * Create a new response message.
	 * 
	 * @param m The message needing a response
	 * @return A new response builder
	 */
	public static Message.Builder rs(Message m) {
		var rs = rs(m.getId());
		if (m.getFrom() != 0)
			rs.setTo(m.getFrom());
		if (m.getTo() != 0)
			rs.setFrom(m.getTo());
		return rs;
	}

	/**
	 * Create a new request message.
	 * 
	 * @param payload The request payload
	 * @return A new request builder
	 */
	public static Message.Builder rq(MessageOrBuilder payload) {
		return setPayload(rq(), payload);
	}

	/**
	 * Create a new response message.
	 * 
	 * @param msg     The message needing a response
	 * @param payload The response payload
	 * @return A new response builder
	 */
	public static Message.Builder rs(Message msg, MessageOrBuilder payload) {
		return setPayload(rs(msg), payload);
	}

	/**
	 * Set the payload of the given message.
	 * 
	 * @param msg     The message to receive the payload
	 * @param payload The payload to insert
	 * @return {@code msg}
	 */
	public static Message.Builder setPayload(Message.Builder msg, MessageOrBuilder payload) {

		// Build the payload if not already built
		if (payload instanceof Builder)
			payload = ((Builder) payload).build();

		// Handle special case for Outcome
		if (payload instanceof Outcome)
			return msg.setRsOutcome((Outcome) payload);

		FieldDescriptor field = Message.getDescriptor()
				.findFieldByName(convertMessageClassToFieldName(payload.getClass()));

		if (field != null) {
			return msg.setField(field, payload);
		} else {
			// Assume this is a plugin message
			// TODO use package for url prefix rather than the full classname
			return msg.setPlugin(Any.pack((com.google.protobuf.Message) payload, payload.getClass().getName()));
		}
	}

	/**
	 * Get the payload from the given message.
	 * 
	 * @param msg The message
	 * @return The message's payload or {@code null} if none
	 */
	public static Object getPayload(Message msg) {
		FieldDescriptor oneof = msg.getOneofFieldDescriptor(MSG_ONEOF);
		if (oneof == null)
			return null;

		var value = msg.getField(oneof);

		// Plugin messages need to be unwrapped
		if (value instanceof Any) {
			var any = (Any) value;
			try {
				return any.unpack(convertPluginUrl(any.getTypeUrl()));
			} catch (InvalidProtocolBufferException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return value;
	}

	/**
	 * Set the payload of the given stream message.
	 * 
	 * @param msg     The message to receive the payload
	 * @param payload The payload to insert
	 * @return {@code msg}
	 */
	public static EV_StreamData.Builder setPayload(EV_StreamData.Builder msg, MessageOrBuilder payload) {

		// Build the payload if not already built
		if (payload instanceof Builder)
			payload = ((Builder) payload).build();

		String name = payload.getClass().getSimpleName();
		FieldDescriptor field = EV_StreamData.getDescriptor()
				.findFieldByName(name.substring(0, name.indexOf("StreamData")).toLowerCase());

		if (field != null) {
			return msg.setField(field, payload);
		} else {
			// Assume this is a plugin message
			return msg.setPlugin(((com.google.protobuf.Message) payload).toByteString());
		}
	}

	/**
	 * Get the payload from the given stream message.
	 * 
	 * @param msg The stream message
	 * @return The message's payload or {@code null} if none
	 */
	public static Object getPayload(EV_StreamData msg) {
		FieldDescriptor oneof = msg.getOneofFieldDescriptor(STREAM_ONEOF);
		if (oneof == null)
			return null;

		return msg.getField(oneof);
	}

	/**
	 * Load a plugin message class from its Any URL.
	 * 
	 * @param url The message URL
	 * @return The loaded message class
	 * @throws ClassNotFoundException
	 */
	private static Class<? extends Message> convertPluginUrl(String url) throws ClassNotFoundException {
		return (Class<? extends Message>) Class.forName(url.split("/")[0]);
	}

	/**
	 * Convert a message class name (RQ_ExampleMessage) to its message field name
	 * (rq_example_message).
	 * 
	 * @param payload The payload class
	 * @return The field name
	 */
	public static String convertMessageClassToFieldName(Class<?> payload) {
		String field = payload.getSimpleName();
		Preconditions.checkArgument(field.charAt(2) == '_');

		return field.substring(0, field.indexOf('_') + 1).toLowerCase()
				+ CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.substring(field.indexOf('_') + 1));
	}

	private ProtoUtil() {
	}
}
