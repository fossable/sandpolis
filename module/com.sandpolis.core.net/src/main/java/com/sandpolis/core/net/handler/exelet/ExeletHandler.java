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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.command.Exelet.Auth;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.command.Exelet.Permission;
import com.sandpolis.core.net.command.Exelet.Unauth;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.MSG;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * A handler that distributes incoming messages to the appropriate
 * {@link Exelet} handler.
 *
 * <p>
 * This handler maintains a separate dispatch vector for each loaded plugin.
 */
public final class ExeletHandler extends SimpleChannelInboundHandler<MSG.Message> {

	private static final Logger log = LoggerFactory.getLogger(ExeletHandler.class);

	private static final Map<Class<? extends Exelet>, Map<Method, MethodHandle>> cache = new HashMap<>();

	private final Sock sock;

	private final DispatchVector coreVector;

	private final Map<String, PluginDispatchVector> pluginVectors;

	private final Map<Class<? extends Exelet>, String> exelets;

	public ExeletHandler(Sock sock) {
		this.sock = sock;
		this.pluginVectors = new HashMap<>();
		this.coreVector = new DispatchVector(sock);
		this.exelets = new HashMap<>();
	}

	public ExeletHandler(Sock sock, Class<? extends Exelet>[] e) {
		this(sock);
		register(e);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG.Message msg) throws Exception {

		switch (msg.getPayloadCase()) {
		case PLUGIN:
			var pluginVector = pluginVectors.get(msg.getPlugin().getTypeUrl().split("/")[0]);
			if (pluginVector != null && pluginVector.accept(msg))
				return;
			break;
		default:
			if (coreVector.accept(msg))
				return;
			break;
		}

		// There's no valid handler
		ctx.fireChannelRead(msg);
	}

	public void register(@SuppressWarnings("unchecked") Class<? extends Exelet>... classes) {
		register("", MSG.Message.class, classes);
	}

	public synchronized void register(String pluginId, Class<? extends Message> messageType,
			@SuppressWarnings("unchecked") Class<? extends Exelet>... classes) {
		try {
			for (var _class : classes) {
				exelets.put(_class, pluginId);

				if (!pluginId.isEmpty()) {
					pluginVectors.put(exelets.get(_class), new PluginDispatchVector(sock, pluginId, messageType));
				}

				var methods = cache.get(_class);
				if (methods == null) {
					methods = new HashMap<>();
					for (Method m : _class.getMethods()) {
						if (m.getAnnotation(Handler.class) != null) {
							methods.put(m, MethodHandles.publicLookup().unreflect(m));
						}
					}

					cache.put(_class, methods);
				}

				for (Method m : methods.keySet()) {
					if (m.isAnnotationPresent(Unauth.class)) {
						engage(_class, m);
					} else if (m.isAnnotationPresent(Auth.class) && sock.isAuthenticated()) {
						engage(_class, m);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void deregister(@SuppressWarnings("unchecked") Class<? extends Exelet>... classes) {
		for (var _class : classes) {
			for (Method m : _class.getMethods()) {
				if (m.isAnnotationPresent(Handler.class)) {
					disengage(_class, m);
				}
			}
		}
	}

	public synchronized void authenticate() {
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
							engage(_class, m);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void deauthenticate() {
		for (Class<? extends Exelet> _class : exelets.keySet()) {
			for (Method m : _class.getMethods()) {
				if (m.isAnnotationPresent(Auth.class)) {
					disengage(_class, m);
				}
			}
		}
	}

	private void engage(Class<? extends Exelet> exelet, Method method) throws Exception {
		if (exelets.get(exelet).isEmpty()) {
			log.trace("Engaging core handler: {}", method.getName());
			coreVector.engage(method, cache.get(exelet).get(method));

		} else {
			log.trace("Engaging plugin handler: {}", method.getName());
			pluginVectors.get(exelets.get(exelet)).engage(method, cache.get(exelet).get(method));
		}
	}

	/**
	 * Immediately remove the handler corresponding to the given plugin and exelet
	 * method.
	 *
	 * @param method The handler method
	 */
	private void disengage(Class<? extends Exelet> exelet, Method method) {
		var handler = method.getAnnotation(Handler.class);
		if (handler == null)
			// This shouldn't happen unless engage is wrong
			throw new RuntimeException("Missing required @Handler annotation on: " + method.getName());

		if (exelets.get(exelet).isEmpty()) {
			log.trace("Disengaging core handler: {}", method.getName());
			coreVector.disengage(handler);
		} else {
			log.trace("Disengaging plugin handler: {}", method.getName());
			pluginVectors.get(exelets.get(exelet)).disengage(handler);
		}
	}
}
