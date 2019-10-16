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

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.util.ProtoUtil;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * Distributes messages to their appropriate handlers. Each plugin get a
 * specialized {@link PluginDispatchVector} capable of handling plugin messages.
 *
 * @author cilki
 * @since 5.1.0
 */
public class DispatchVector {

	private static final Logger log = LoggerFactory.getLogger(DispatchVector.class);

	/**
	 * The maximum handler tag. This is here to avoid wasting a lot of memory if a
	 * message is misconfigured.
	 */
	private static final int TAG_LIMIT = 256;

	private final Sock sock;

	protected Consumer<MSG>[] vector;

	/**
	 * Build a non-plugin {@link DispatchVector}.
	 */
	@SuppressWarnings("unchecked")
	public DispatchVector(Sock sock) {
		this.sock = Objects.requireNonNull(sock);
		this.vector = new Consumer[0];
	}

	/**
	 * Obtain the payload from a container message.
	 *
	 * @param msg The container message
	 * @return The message's payload
	 * @throws InvalidProtocolBufferException
	 */
	protected Message unwrapPayload(MSG msg) throws InvalidProtocolBufferException {
		return ProtoUtil.getPayload(msg);
	}

	protected MSG wrapPayload(MSG msg, MessageOrBuilder payload) {
		// Build the payload if not already built
		if (payload instanceof Builder)
			payload = ((Builder) payload).build();

		// Handle special case for Outcome
		if (payload instanceof Outcome)
			return ProtoUtil.rs(msg).setRsOutcome((Outcome) payload).build();

		FieldDescriptor field = MSG.getDescriptor()
				.findFieldByName(ProtoUtil.convertMessageClassToFieldName(payload.getClass()));

		return ProtoUtil.rs(msg).setField(field, payload).build();
	}

	/**
	 * Dispatch an incoming message.
	 *
	 * @param msg The incoming message
	 * @return Whether the message was handled
	 * @throws Exception
	 */
	public boolean accept(MSG msg) throws Exception {
		return dispatch(msg, msg.getPayloadCase().getNumber());
	}

	protected boolean dispatch(MSG msg, int tag) {
		if (vector.length > tag) {
			var handler = vector[tag];
			if (handler != null) {
				// Execute the message with the handler
				handler.accept(msg);
				return true;
			}
		}

		return false;
	}

	/**
	 * Immediately remove the handler corresponding to the given {@link Handler}.
	 *
	 * @param handler The handler to remove
	 */
	public synchronized void disengage(Handler handler) {
		vector[handler.tag()] = null;
	}

	/**
	 * Immediately add the handler corresponding to the given plugin and exelet
	 * method.
	 *
	 * @param method The message handler
	 * @param handle The method handle
	 */
	public synchronized void engage(Method method, MethodHandle handle) throws Exception {
		var handler = method.getAnnotation(Handler.class);
		if (handler == null)
			throw new RuntimeException("Missing required @Handler annotation on: " + method.getName());

		ensureSize(handler);

		// TYPE 2
		if (method.getReturnType() == MessageOrBuilder.class && method.getParameterCount() == 1
				&& Message.class.isAssignableFrom(method.getParameterTypes()[0])) {

			vector[handler.tag()] = msg -> {
				try {
					var test = unwrapPayload(msg);
					MessageOrBuilder rs = (MessageOrBuilder) handle.invoke(test);
					if (rs != null)
						sock.send(wrapPayload(msg, rs));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}

			};
		}

		// TYPE 1
		else if (method.getReturnType() == void.class && method.getParameterCount() == 1
				&& Message.class.isAssignableFrom(method.getParameterTypes()[0])) {

			vector[handler.tag()] = msg -> {
				try {
					handle.invoke(unwrapPayload(msg));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// No error response because this handler doesn't send a response normally
				}
			};
		}

		// TYPE 3
		else if (method.getReturnType() == void.class && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& Message.class.isAssignableFrom(method.getParameterTypes()[1])) {

			vector[handler.tag()] = msg -> {
				ExeletContext context = new ExeletContext(sock, msg);
				try {
					handle.invoke(context, unwrapPayload(msg));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}

				if (context.reply != null)
					sock.send(wrapPayload(msg, context.reply));
				if (context.deferAction != null) {
					try {
						context.deferAction.run();
					} catch (Exception e) {
						log.error("Failed to run deferred action", e);
					}
				}
			};
		}

		// TYPE 4
		else if (method.getReturnType() == MessageOrBuilder.class && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& Message.class.isAssignableFrom(method.getParameterTypes()[1])) {

			vector[handler.tag()] = msg -> {
				ExeletContext context = new ExeletContext(sock, msg);
				try {
					MessageOrBuilder rs = (MessageOrBuilder) handle.invoke(context, unwrapPayload(msg));
					if (rs != null)
						sock.send(wrapPayload(msg, rs));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}

				if (context.deferAction != null) {
					try {
						context.deferAction.run();
					} catch (Exception e) {
						log.error("Failed to run deferred action", e);
					}
				}
			};
		}

		// Unknown format
		else
			throw new RuntimeException("Unknown handler format for method: " + method.getName());
	}

	/**
	 * Ensure that the internal vector is large enough to contain the given handler.
	 * If not, expand the vector.
	 *
	 * @param handler The handler descriptor
	 */
	private synchronized void ensureSize(Handler handler) {
		checkArgument(handler.tag() < TAG_LIMIT, "Handler tag too large");
		checkArgument(handler.tag() > 0, "Negative handler tag");

		if (handler.tag() >= vector.length)
			vector = Arrays.copyOf(vector, handler.tag() + 1);
	}
}
