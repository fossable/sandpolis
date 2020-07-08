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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.util.MsgUtil;

/**
 * This class distributes messages to their appropriate handlers.
 *
 * @author cilki
 * @since 5.1.0
 */
public final class DispatchMap {

	private static final Logger log = LoggerFactory.getLogger(DispatchMap.class);

	private static final Map<String, MethodHandle> methodHandleCache = Collections.synchronizedMap(new HashMap<>());

	private final Connection sock;

	protected final Map<String, Consumer<MSG>> handlers;

	public DispatchMap(Connection sock) {
		this.sock = Objects.requireNonNull(sock);
		this.handlers = new HashMap<>();
	}

	/**
	 * Dispatch an incoming message.
	 *
	 * @param msg The incoming message
	 * @return Whether the message was handled
	 */
	public boolean accept(MSG msg) {
		var handler = handlers.get(msg.getPayload().getTypeUrl());
		if (handler != null) {
			// Execute the message with the handler
			handler.accept(msg);
			return true;
		}

		return false;
	}

	/**
	 * Immediately remove the handler corresponding to the given {@link Handler}.
	 *
	 * @param handler The handler to remove
	 */
	public synchronized void disengage(Method handler) {
		handlers.remove(MsgUtil.getTypeUrl(handler));
	}

	/**
	 * Immediately add the handler corresponding to the given plugin and exelet
	 * method.
	 *
	 * @param method The message handler
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized void engage(Method method) throws Exception {
		String typeUrl = MsgUtil.getTypeUrl(method);
		if (!methodHandleCache.containsKey(typeUrl)) {
			methodHandleCache.put(typeUrl, MethodHandles.publicLookup().unreflect(method));
		}

		MethodHandle handle = methodHandleCache.get(typeUrl);

		// TYPE 2
		if (method.getReturnType() == MessageOrBuilder.class && method.getParameterCount() == 1
				&& Message.class.isAssignableFrom(method.getParameterTypes()[0])) {

			handlers.put(typeUrl, msg -> {
				try {
					MessageOrBuilder rs = (MessageOrBuilder) handle
							.invoke(msg.getPayload().unpack((Class) method.getParameterTypes()[0]));

					if (rs != null)
						sock.send(MsgUtil.rs(msg, rs));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}
			});
		}

		// TYPE 1
		else if (method.getReturnType() == void.class && method.getParameterCount() == 1
				&& Message.class.isAssignableFrom(method.getParameterTypes()[0])) {

			handlers.put(typeUrl, msg -> {
				try {
					handle.invoke(msg.getPayload().unpack((Class) method.getParameterTypes()[0]));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// No error response because this handler doesn't send a response normally
				}
			});
		}

		// TYPE 3
		else if (method.getReturnType() == void.class && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& Message.class.isAssignableFrom(method.getParameterTypes()[1])) {

			handlers.put(typeUrl, msg -> {
				ExeletContext context = new ExeletContext(sock, msg);
				try {
					handle.invoke(context, msg.getPayload().unpack((Class) method.getParameterTypes()[1]));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}

				if (context.reply != null)
					sock.send(MsgUtil.rs(msg, context.reply));
				if (context.deferAction != null) {
					try {
						context.deferAction.run();
					} catch (Exception e) {
						log.error("Failed to run deferred action", e);
					}
				}
			});
		}

		// TYPE 4
		else if (method.getReturnType() == MessageOrBuilder.class && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& Message.class.isAssignableFrom(method.getParameterTypes()[1])) {

			handlers.put(typeUrl, msg -> {
				ExeletContext context = new ExeletContext(sock, msg);
				try {
					MessageOrBuilder rs = (MessageOrBuilder) handle.invoke(context,
							msg.getPayload().unpack((Class) method.getParameterTypes()[1]));
					if (rs != null)
						sock.send(MsgUtil.rs(msg, rs));
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
			});
		}

		// Unknown format
		else
			throw new RuntimeException("Unknown handler format for method: " + method.getName());
	}
}
