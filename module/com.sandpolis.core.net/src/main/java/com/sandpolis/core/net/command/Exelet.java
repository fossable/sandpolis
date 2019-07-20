/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.core.net.command;

import static com.sandpolis.core.util.ProtoUtil.rs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ProtoUtil;

/**
 * An {@link Exelet} handles incoming messages from a {@link Sock}.<br>
 * <br>
 * There are two types of message handlers:
 * <ul>
 * <li>Simple</li>
 * <li>Request Shortcut</li>
 * </ul>
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class Exelet {

	/**
	 * The remote endpoint.
	 */
	protected Sock connector;

	/**
	 * Defines the message type that the target {@link Exelet} method handles.
	 * 
	 * @author cilki
	 * @since 5.1.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Handler {

		/**
		 * The message tag of the message that is handled.
		 * 
		 * @return The message tag
		 */
		public int tag();
	}

	/**
	 * When applied to an {@link Exelet} method, the method will be executable on
	 * authenticated connections only.
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Auth {
	}

	/**
	 * When applied to an {@link Exelet} method, the method will be executable on
	 * unauthenticated connections only.
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Unauth {
	}

	/**
	 * When applied to an {@link Exelet} method, the method will be executable on
	 * connections which have the necessary permission.
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Permission {
		short permission();
	}

	/**
	 * Indicates that the method makes an access decision for another method and is
	 * not directly executable.
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public static @interface AccessPredicate {
	}

	public void setConnector(Sock connector) {
		this.connector = Objects.requireNonNull(connector);
	}

	/**
	 * Get the {@link Exelet}'s plugin message URL prefix.
	 * 
	 * @return The URL prefix or {@code null} if the exelet is not for plugins
	 */
	public String getPluginPrefix() {
		return null;
	}

	/**
	 * Get the given message's payload. Plugin exelets should override this method.
	 * 
	 * @param msg The message
	 * @return The message's payload
	 */
	public Message extractPayload(MSG.Message msg) {
		return ProtoUtil.getPayload(msg);
	}

	/**
	 * Send a response to a message using {@link Sock#send}.
	 * 
	 * @param m        The original message
	 * @param response The response payload
	 */
	public void reply(MSG.Message m, MessageOrBuilder response) {
		connector.send(rs(m, response));
	}

	/**
	 * Begin a message handler.
	 * 
	 * @return A new intermediate outcome
	 */
	protected Outcome.Builder begin() {
		return ProtoUtil.begin();
	}

	/**
	 * Complete the given handler outcome as failed.
	 * 
	 * @param outcome The handler outcome
	 * @return {@code outcome}
	 */
	protected Outcome.Builder failure(Outcome.Builder outcome) {
		return outcome.setTime(System.currentTimeMillis() - outcome.getTime()).setResult(false);
	}

	/**
	 * Complete the given handler outcome as failed.
	 * 
	 * @param outcome The handler outcome
	 * @param code    The error code
	 * @return {@code outcome}
	 */
	protected Outcome.Builder failure(Outcome.Builder outcome, ErrorCode code) {
		return outcome.setTime(System.currentTimeMillis() - outcome.getTime()).setResult(false).setError(code);
	}

	/**
	 * Complete the given handler outcome as succeeded.
	 * 
	 * @param outcome The handler outcome
	 * @return {@code outcome}
	 */
	protected Outcome.Builder success(Outcome.Builder outcome) {
		return outcome.setTime(System.currentTimeMillis() - outcome.getTime()).setResult(true);
	}

	/**
	 * Complete the given handler outcome (as failed or succeeded depending on the
	 * error code).
	 * 
	 * @param outcome The handler outcome
	 * @return {@code outcome}
	 */
	protected Outcome.Builder complete(Outcome.Builder outcome, ErrorCode code) {
		return code == ErrorCode.OK ? success(outcome) : failure(outcome, code);
	}

}
