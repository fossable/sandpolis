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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.command.Exelet.Auth;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.command.Exelet.Permission;
import com.sandpolis.core.net.command.Exelet.Unauth;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.net.MSG.Message.MsgOneofCase;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ProtoUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ExeletHandler extends SimpleChannelInboundHandler<MSG.Message> {

	private static final Logger log = LoggerFactory.getLogger(ExeletHandler.class);

	private Sock sock;

	public void setSock(Sock sock) {
		this.sock = sock;
		initUnauth();
	}

	/**
	 * A list of {@link Exelet}s available to this handler.
	 */
	private final Map<Class<? extends Exelet>, Exelet> exelets;

	private Map<String, Consumer<MSG.Message>[]> pluginHandlers;

	/**
	 * Maps message tags (indicies) to the relevant handler. A specific handler will
	 * only be present in the list if its annotated requirements (permissions and
	 * authentication state) are met. Therefore, invalid messages will always be
	 * ignored.
	 */
	@SuppressWarnings("unchecked")
	private Consumer<MSG.Message>[] coreHandlers = new Consumer[0];

	/**
	 * Create a new {@link ExeletHandler} with the given {@link Exelet} list.
	 * 
	 * @param exelets A list of available {@link Exelet}s or {@code null} for none
	 */
	public ExeletHandler(Class<? extends Exelet>[] exelets) {
		this.pluginHandlers = new HashMap<>();
		this.exelets = new HashMap<>();

		if (exelets != null)
			for (Class<? extends Exelet> exelet : exelets)
				// Use null values to indicate that an exelet has not been loaded yet
				this.exelets.put(exelet, null);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG.Message msg) throws Exception {
		var msgType = msg.getMsgOneofCase();

		if (msgType == MsgOneofCase.PLUGIN) {
			String url = msg.getPlugin().getTypeUrl();
			var handlers = pluginHandlers.get(url.substring(0, url.indexOf('/')));
			if (handlers != null) {
				Message plugin = ProtoUtil.getPayload(msg);
				var pluginType = plugin.getClass().getMethod("getPluginTypeCase").invoke(plugin);
				int pluginNumber = (int) pluginType.getClass().getMethod("getNumber").invoke(pluginType);

				if (handlers.length > pluginNumber) {
					Consumer<MSG.Message> handle = handlers[pluginNumber];
					if (handle != null) {
						// Execute the message with the predefined handler
						handle.accept(msg);
						return;
					}
				}
			}
		} else if (msgType != MsgOneofCase.MSGONEOF_NOT_SET) {
			if (coreHandlers.length > msgType.getNumber()) {
				var handle = coreHandlers[msgType.getNumber()];
				if (handle != null) {
					// Execute the message with the predefined handler
					handle.accept(msg);
					return;
				}
			}
		}

		ctx.fireChannelRead(msg);
	}

	/**
	 * Setup the handler for unauthenticated messages. Permission annotations are
	 * ignored for unauth handlers.
	 * 
	 * @param sock The connector for each {@link Exelet}
	 */
	public void initUnauth() {
		try {
			for (Class<? extends Exelet> _class : exelets.keySet()) {
				for (Method m : _class.getMethods()) {
					if (m.isAnnotationPresent(Unauth.class)) {
						register(_class, m);
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
	public void initAuth() {
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
							register(_class, m);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void deauth() {
		// TODO
	}

	/**
	 * Associate a message type with a handler. In the event of multiple
	 * registrations of the same message type, the last registration wins.
	 * 
	 * @param sock   The connector for the {@link Exelet} instance if necessary
	 * @param _class The {@link Exelet} class that contains the message handler
	 * @param method The message handler
	 */
	private void register(Class<? extends Exelet> _class, Method method) throws Exception {
		var handler = method.getAnnotation(Handler.class);
		if (handler == null)
			throw new RuntimeException("Missing required @Handler annotation on: " + method.getName());

		// Build Exelet instance
		final Exelet exelet;
		if (exelets.get(_class) != null) {
			exelet = exelets.get(_class);
		} else {
			// Build a new exelet for the given class
			exelet = _class.getConstructor().newInstance();
			exelet.setConnector(sock);
			exelets.put(_class, exelet);
		}

		// Select a handler vector
		Consumer<MSG.Message>[] handlers = null;
		if (exelet.getPluginPrefix() == null) {
			// Ensure size of core vector
			if (coreHandlers.length <= handler.tag())
				coreHandlers = Arrays.copyOf(coreHandlers, handler.tag() + 1);
			handlers = coreHandlers;
		} else {
			// Ensure size of plugin vector
			handlers = pluginHandlers.get(exelet.getPluginPrefix());
			if (handlers == null) {
				handlers = new Consumer[handler.tag() + 1];
				pluginHandlers.put(exelet.getPluginPrefix(), handlers);
			} else if (handlers.length <= handler.tag()) {
				handlers = Arrays.copyOf(handlers, handler.tag() + 1);
				pluginHandlers.put(exelet.getPluginPrefix(), handlers);
			}
		}

		// Simple request style: public Message.Builder rq_command(RQ_Command rq)
		if (method.getReturnType() == Message.Builder.class && method.getParameterCount() == 1) {
			MethodHandle handle = MethodHandles.publicLookup().bind(exelet, method.getName(),
					MethodType.methodType(method.getReturnType(), method.getParameterTypes()[0]));

			handlers[handler.tag()] = msg -> {
				try {
					Message.Builder rs = (Message.Builder) handle.invoke(ProtoUtil.getPayload(msg));
					exelet.reply(msg, rs);
				} catch (Throwable e) {
					log.warn("Message handler failed", e);
					sock.send(ProtoUtil.rs((MSG.Message) msg, Outcome.newBuilder().setResult(false)));
				}
			};
		}

		// Void request style: public void rq_command(Message rq)
		else if (method.getReturnType() == void.class && method.getParameterCount() == 1
				&& method.getParameterTypes()[0] == MSG.Message.class) {
			MethodHandle handle = MethodHandles.publicLookup().bind(exelet, method.getName(),
					MethodType.methodType(method.getReturnType(), method.getParameterTypes()[0]));

			handlers[handler.tag()] = msg -> {
				try {
					handle.invoke(msg);
				} catch (Throwable e) {
					log.warn("Message handler failed", e);
				}
			};
		}

		// Unknown format
		else
			throw new RuntimeException("Unknown handler format for method: " + method.getName());
	}
}
