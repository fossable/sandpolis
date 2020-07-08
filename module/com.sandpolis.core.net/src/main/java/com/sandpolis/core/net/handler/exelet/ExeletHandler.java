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

import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.command.Exelet.Auth;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.command.Exelet.Permission;
import com.sandpolis.core.net.command.Exelet.Unauth;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.core.net.util.MsgUtil;

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

	private final Connection sock;

	private final Map<String, DispatchMap> dispatchers;

	private final List<Class<? extends Exelet>> exelets;

	public ExeletHandler(Connection sock) {
		this.sock = sock;
		this.dispatchers = new HashMap<>();
		this.exelets = new ArrayList<>();

		// Register plugin exelets
		PluginStore.getLoadedPlugins().forEach(plugin -> {
			plugin.getExtensions(ExeletProvider.class).forEach(provider -> {
				register(provider.getExelets());
			});
		});
	}

	public ExeletHandler(Connection sock, Class<? extends Exelet>[] e) {
		this(sock);
		register(e);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MSG msg) throws Exception {

		var dispatcher = dispatchers.get(msg.getPayload().getTypeUrl().split("/")[0]);
		if (dispatcher != null) {
			dispatcher.accept(msg);
			return;
		}

		// There's no valid dispatcher
		ctx.fireChannelRead(msg);
	}

	public synchronized void register(@SuppressWarnings("unchecked") Class<? extends Exelet>... classes) {
		try {
			for (var _class : classes) {
				exelets.add(_class);

				for (Method m : _class.getMethods()) {
					if (m.getAnnotation(Handler.class) != null) {
						if (m.isAnnotationPresent(Unauth.class)
								|| (m.isAnnotationPresent(Auth.class) && sock.isAuthenticated())) {

							DispatchMap dv = dispatchers.get(MsgUtil.getModuleId(m));
							if (dv == null) {
								dv = new DispatchMap(sock);
								dispatchers.put(MsgUtil.getModuleId(m), dv);
							}
							dv.engage(m);
						}
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
					dispatchers.get(MsgUtil.getModuleId(m)).disengage(m);
				}
			}
		}
	}

	public synchronized void authenticate() {
		try {
			for (Class<? extends Exelet> _class : exelets) {
				for (Method m : _class.getMethods()) {
					if (m.isAnnotationPresent(Auth.class)) {
						boolean passed = true;

						// Check permissions
						for (Permission a : m.getAnnotationsByType(Permission.class)) {
							// TODO
						}

						if (passed) {
							dispatchers.get(MsgUtil.getModuleId(m)).engage(m);
						}

					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void deauthenticate() {
		for (Class<? extends Exelet> _class : exelets) {
			for (Method m : _class.getMethods()) {
				if (m.isAnnotationPresent(Auth.class)) {
					MsgUtil.getModuleId(m);
				}
			}
		}
	}
}
