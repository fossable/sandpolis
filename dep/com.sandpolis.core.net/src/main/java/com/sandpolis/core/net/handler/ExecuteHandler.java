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
package com.sandpolis.core.net.handler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Exelet.Auth;
import com.sandpolis.core.net.Exelet.Permission;
import com.sandpolis.core.net.Exelet.Unauth;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.net.MSG.Message.MsgOneofCase;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * This mutable handler dispatches messages to their handlers or to a waiting
 * Thread via a {@link MessageFuture}. Messages that don't have a registered
 * handler or have a Thread waiting for them are dropped.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class ExecuteHandler extends SimpleChannelInboundHandler<Message> {

	private static final Logger log = LoggerFactory.getLogger(ExecuteHandler.class);

	/**
	 * A list of {@link Exelet}s available to this handler.
	 */
	private final Map<Class<? extends Exelet>, Exelet> exelets;

	/**
	 * Maps message types to the relevant handler. A specific handler will only be
	 * present in the map if its annotated requirements (permissions and
	 * authentication state) are met. Therefore, invalid messages will always be
	 * ignored.
	 */
	private final Map<MsgOneofCase, MethodHandle> handles;

	/**
	 * When a response message is desired, a {@link MessageFuture} is placed into
	 * this map. If a message is received which is not associated with any handler
	 * in {@link #handles} and the message's ID is in {@link #responseMap}, the
	 * MessageFuture is removed and notified.
	 */
	private final ConcurrentMap<Integer, MessageFuture> responseMap;

	/**
	 * Create a new {@link ExecuteHandler} with the given {@link Exelet} list.
	 * 
	 * @param exelets A list of available {@link Exelet}s
	 */
	public ExecuteHandler(Class<? extends Exelet>[] exelets) {
		if (exelets == null)
			throw new IllegalArgumentException();

		this.responseMap = new ConcurrentHashMap<>();
		this.handles = new HashMap<>();
		this.exelets = new HashMap<>();

		for (Class<? extends Exelet> exelet : exelets)
			// Use null values to indicate that an exelet has not been loaded yet
			this.exelets.put(exelet, null);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {

		MethodHandle handle = handles.get(msg.getMsgOneofCase());
		if (handle != null) {
			// Execute the message with the predefined handler
			try {
				handle.invoke(msg);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			return;
		}

		MessageFuture future = responseMap.remove(msg.getId());
		if (future != null) {
			// Give the message to a waiting Thread
			future.setSuccess(msg);
			return;
		}

		// Drop the message
		log.warn("Dropping a message of type: {}", msg.getMsgOneofCase());
		if (log.isDebugEnabled())
			log.debug("Dropped message: {}", msg.toString());
	}

	/**
	 * Add a new response callback to the response map unless one is already
	 * present.
	 * 
	 * @param id     The message ID
	 * @param future A new {@link MessageFuture}
	 * @return An existing future or the given parameter
	 */
	public MessageFuture putResponseFuture(int id, MessageFuture future) {
		if (!responseMap.containsKey(id))
			responseMap.put(id, future);

		return responseMap.get(id);
	}

	/**
	 * Get the number of futures waiting for a response.
	 * 
	 * @return The number of entries in the response map
	 */
	public int getResponseCount() {
		return responseMap.size();
	}

	/**
	 * Indicates whether the {@code ExecuteHandler} has a registered handler for the
	 * given message type.
	 * 
	 * @param type The message type
	 * @return True if there exists an {@link Exelet} handler for the message type
	 */
	public boolean containsHandler(MsgOneofCase type) {
		return handles.containsKey(type);
	}

	/**
	 * Reset all {@link Exelet} handlers.
	 */
	public void resetHandlers() {
		// TODO disable channel reads
		handles.clear();
	}

	/**
	 * Setup the handler for unauthenticated messages. Permission annotations are
	 * ignored for unauth handlers.
	 * 
	 * @param sock The connector for each {@link Exelet}
	 */
	public void initUnauth(Sock sock) {
		try {
			for (Class<? extends Exelet> _class : exelets.keySet()) {
				for (Method m : _class.getMethods()) {
					if (m.isAnnotationPresent(Unauth.class)) {
						register(sock, _class, m);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Setup the handler for authenticated messages. Permission annotations will be
	 * checked.
	 * 
	 * @param sock The connector for each {@link Exelet}
	 */
	public void initAuth(Sock sock) {
		try {
			for (Class<? extends Exelet> _class : exelets.keySet()) {
				for (Method m : _class.getMethods()) {
					if (m.isAnnotationPresent(Auth.class)) {
						boolean passed = true;

						// Check permissions
						for (Permission a : m.getAnnotationsByType(Permission.class)) {
							// TODO
						}

						if (passed)
							register(sock, _class, m);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Associate a message type with a handler. In the event of multiple
	 * registrations of the same message type, the last registration wins.
	 * 
	 * @param sock   The connector for the {@link Exelet} instance if necessary
	 * @param _class The {@link Exelet} class that contains the message handler
	 * @param method The message handler
	 */
	private void register(Sock sock, Class<? extends Exelet> _class, Method method) throws Exception {
		// Retrieve the message type
		MsgOneofCase type = MsgOneofCase.valueOf(method.getName().toUpperCase());
		if (type == MsgOneofCase.MSGONEOF_NOT_SET)
			throw new RuntimeException();

		Exelet exelet = exelets.get(_class);
		if (exelet == null) {
			// Build a new exelet for the given class
			exelet = _class.getConstructor(Sock.class).newInstance(sock);
			exelets.put(_class, exelet);
		}

		// A direct reference to the message handler
		MethodHandle handle = MethodHandles.publicLookup().bind(exelet, method.getName(),
				MethodType.methodType(void.class, Message.class));

		handles.put(type, handle);
	}

}
