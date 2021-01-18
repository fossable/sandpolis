//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.exelet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLiteOrBuilder;
import com.sandpolis.core.instance.state.ConnectionOid;
import com.sandpolis.core.net.util.MsgUtil;

public class ExeletMethod {

	private static final Logger log = LoggerFactory.getLogger(ExeletMethod.class);

	public final Exelet.Handler metadata;

	public final int type;

	public final String name;

	private Consumer<ExeletContext> handler;

	/**
	 * Consume an {@link ExeletContext}.
	 *
	 * @param context The context which wraps a request
	 */
	public void accept(ExeletContext context) {
		handler.accept(context);
	}

	public ExeletMethod(Method method) throws Exception {
		metadata = checkNotNull(method.getAnnotation(Exelet.Handler.class), "Method not a handler");
		name = method.getName();

		// Obtain a handle to the static method
		var handle = MethodHandles.publicLookup().unreflect(method);

		// TYPE 2
		if (method.getReturnType() == MessageLiteOrBuilder.class && method.getParameterCount() == 1
				&& MessageLite.class.isAssignableFrom(method.getParameterTypes()[0])) {

			var parseFrom = MethodHandles.publicLookup()
					.unreflect(method.getParameterTypes()[0].getMethod("parseFrom", ByteString.class));

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED) && metadata.auth()) {
					return;
				}

				try {
					var rs = (MessageLiteOrBuilder) handle.invoke(parseFrom.invoke(context.request.getPayload()));

					if (rs != null)
						context.connector.send(MsgUtil.rs(context.request, rs));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}
			};
		}

		// TYPE 1
		else if (method.getReturnType() == void.class && method.getParameterCount() == 1
				&& MessageLite.class.isAssignableFrom(method.getParameterTypes()[0])) {

			var parseFrom = MethodHandles.publicLookup()
					.unreflect(method.getParameterTypes()[0].getMethod("parseFrom", ByteString.class));

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED) && metadata.auth()) {
					return;
				}

				try {
					handle.invoke(parseFrom.invoke(context.request.getPayload()));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// No error response because this handler doesn't send a response normally
				}
			};
		}

		// TYPE 3
		else if (method.getReturnType() == void.class && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& MessageLite.class.isAssignableFrom(method.getParameterTypes()[1])) {

			var parseFrom = MethodHandles.publicLookup()
					.unreflect(method.getParameterTypes()[1].getMethod("parseFrom", ByteString.class));

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED) && metadata.auth()) {
					return;
				}

				try {
					handle.invoke(context, parseFrom.invoke(context.request.getPayload()));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}

				if (context.reply != null)
					context.connector.send(MsgUtil.rs(context.request, context.reply));
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
		else if (method.getReturnType() == MessageLiteOrBuilder.class && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& MessageLite.class.isAssignableFrom(method.getParameterTypes()[1])) {

			var parseFrom = MethodHandles.publicLookup()
					.unreflect(method.getParameterTypes()[1].getMethod("parseFrom", ByteString.class));

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED) && metadata.auth()) {
					return;
				}

				try {
					var rs = (MessageLiteOrBuilder) handle.invoke(context,
							parseFrom.invoke(context.request.getPayload()));
					if (rs != null)
						context.connector.send(MsgUtil.rs(context.request, rs));
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
			throw new IllegalArgumentException("Unknown handler format for method: " + method.getName());

		// Set this last in case the method was not a valid handler
		type = MsgUtil.getPayloadType(method);
	}

}
