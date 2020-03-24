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

import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.command.Exelet.Auth;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.command.Exelet.Permission;
import com.sandpolis.core.net.command.Exelet.Unauth;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.core.net.sock.Sock;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * A handler that distributes incoming messages to the appropriate
 * {@link Exelet} handler.
 *
 * <p>
 * This handler maintains a separate dispatch vector for each loaded plugin.
 */
public final class ExeletHandler extends SimpleChannelInboundHandler<MSG> {

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

		// Register plugin exelets
		PluginStore.getLoadedPlugins().forEach(plugin -> {
			plugin.getExtensions(ExeletProvider.class).forEach(provider -> {
				register(plugin.getId(), provider.getMessageType(), provider.getExelets());
			});
		});
	}

	public ExeletHandler(Sock sock, Class<? extends Exelet>[] e) {
		this(sock);
		register(e);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {

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
		register("", MSG.class, classes);
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
