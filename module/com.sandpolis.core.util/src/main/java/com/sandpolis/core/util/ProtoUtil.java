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

import com.google.protobuf.Message.Builder;
import com.google.common.base.CaseFormat;
import com.google.protobuf.MessageOrBuilder;
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
	private ProtoUtil() {
	}

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
		return rs(m.getId());
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

		String field = payload.getClass().getSimpleName();
		String prefix = field.substring(0, field.indexOf('_') + 1).toLowerCase();

		// Convert case
		field = prefix
				+ CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.substring(field.indexOf('_') + 1));

		try {
			return msg.setField(Message.getDescriptor().findFieldByName(field), payload);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to find type: " + field, e);
		}
	}

	/**
	 * Get an {@link Outcome} from a message.
	 * 
	 * @param msg The message containing an outcome
	 * @return The outcome
	 */
	public static Outcome getOutcome(Message msg) {
		Outcome.Builder outcome = begin();
		if (msg == null)
			return failure(outcome);
		if (msg.getRsOutcome() == null)
			return failure(outcome);
		return msg.getRsOutcome();
	}

}
