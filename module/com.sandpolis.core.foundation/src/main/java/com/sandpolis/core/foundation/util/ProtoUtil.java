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
package com.sandpolis.core.foundation.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.foundation.Result.Outcome;

/**
 * Utilities for simplifying common operations related to protocol buffers.
 * Using a static import is a convenient way to use these methods.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ProtoUtil {

	/**
	 * Begin an action that should be completed with {@link #success} or
	 * {@link #failure}.
	 *
	 * @return A new incomplete outcome
	 */
	public static Outcome.Builder begin() {
		return Outcome.newBuilder().setTime(System.currentTimeMillis());
	}

	/**
	 * Begin an action that should be completed with {@link #success} or
	 * {@link #failure}.
	 *
	 * @param action The action description
	 * @return A new incomplete outcome
	 */
	public static Outcome.Builder begin(String action) {
		return begin().setAction(action);
	}

	/**
	 * Complete an action with a successful result.
	 *
	 * @param outcome The outcome builder to complete
	 * @return The completed outcome
	 */
	public static Outcome success(Outcome.Builder outcome) {
		return outcome.setResult(true).setTime(System.currentTimeMillis() - outcome.getTime()).build();
	}

	/**
	 * Complete an action with a successful result.
	 *
	 * @param outcome The outcome builder to complete
	 * @param comment The action comment
	 * @return The completed outcome
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
	 * @return The completed outcome
	 */
	public static Outcome failure(Outcome.Builder outcome, String comment) {
		return failure(outcome.setComment(comment));
	}

	/**
	 * Complete an action with an unsuccessful result.
	 *
	 * @param outcome The outcome builder to complete
	 * @param cause   The exception that caused the failure
	 * @return The completed outcome
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
	 * @return The completed outcome
	 */
	public static Outcome failure(Outcome.Builder outcome) {
		return outcome.setResult(false).clearTime().build();
	}

	/**
	 * Complete the given handler outcome as failed.
	 *
	 * @param outcome The handler outcome
	 * @param code    The error code
	 * @return {@code outcome}
	 */
	public static Outcome failure(Outcome.Builder outcome, ErrorCode code) {
		return outcome.setTime(System.currentTimeMillis() - outcome.getTime()).setResult(false).setError(code).build();
	}

	public static Outcome failure(ErrorCode code) {
		return Outcome.newBuilder().setResult(false).setError(code).build();
	}

	/**
	 * Complete an action with an unspecified result.
	 *
	 * @param outcome The outcome builder to complete
	 * @return The completed outcome
	 */
	public static Outcome complete(Outcome.Builder outcome) {
		return outcome.setTime(System.currentTimeMillis() - outcome.getTime()).build();
	}

	/**
	 * Complete the given handler outcome (as failed or succeeded depending on the
	 * error code).
	 *
	 * @param outcome The handler outcome
	 * @return {@code outcome}
	 */
	public static Outcome complete(Outcome.Builder outcome, ErrorCode code) {
		return code == ErrorCode.OK ? success(outcome) : failure(outcome, code);
	}

	/**
	 * Get the payload from the given message.
	 *
	 * @param msg The message
	 * @return The message's payload or {@code null} if empty
	 */
	public static Message getPayload(Message msg) {
		FieldDescriptor oneof = msg.getOneofFieldDescriptor(msg.getDescriptorForType().getOneofs().get(0));
		if (oneof == null)
			return null;

		return (Message) msg.getField(oneof);
	}

	private ProtoUtil() {
	}
}
