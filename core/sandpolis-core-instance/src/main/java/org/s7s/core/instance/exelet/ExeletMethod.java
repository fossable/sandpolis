//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.exelet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.util.S7SMsg;

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
		if ((method.getReturnType() == MessageLiteOrBuilder.class
				|| Enum.class.isAssignableFrom(method.getReturnType())) && method.getParameterCount() == 1
				&& MessageLite.class.isAssignableFrom(method.getParameterTypes()[0])) {

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED).asBoolean() && metadata.auth()) {
					return;
				}

				try {
					var rs = handle.invoke(S7SMsg.of(context.request).unpack(method.getParameterTypes()[0]));

					if (rs instanceof MessageLiteOrBuilder m) {
						context.connector.send(S7SMsg.of(context.request).pack(m));
					} else if (rs instanceof Enum<?> m) {
						context.connector.send(S7SMsg.of(context.request).pack(m));
					}
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}
			};
		}

		// TYPE 1
		else if (method.getReturnType() == void.class && method.getParameterCount() == 1
				&& MessageLite.class.isAssignableFrom(method.getParameterTypes()[0])) {

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED).asBoolean() && metadata.auth()) {
					return;
				}

				try {
					handle.invoke(S7SMsg.of(context.request).unpack(method.getParameterTypes()[0]));
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

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED).asBoolean() && metadata.auth()) {
					return;
				}

				try {
					handle.invoke(context, S7SMsg.of(context.request).unpack(method.getParameterTypes()[1]));
				} catch (Throwable e) {
					log.error("Failed to handle message", e);
					// TODO error outcome
				}

				if (context.reply != null)
					context.connector.send(S7SMsg.of(context.request).pack(context.reply));
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
		else if ((method.getReturnType() == MessageLiteOrBuilder.class
				|| Enum.class.isAssignableFrom(method.getReturnType())) && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& MessageLite.class.isAssignableFrom(method.getParameterTypes()[1])) {

			handler = context -> {
				// Access control
				if (!context.connector.get(ConnectionOid.AUTHENTICATED).asBoolean() && metadata.auth()) {
					return;
				}

				try {
					var rs = handle.invoke(context, S7SMsg.of(context.request).unpack(method.getParameterTypes()[1]));

					if (rs instanceof MessageLiteOrBuilder m) {
						context.connector.send(S7SMsg.of(context.request).pack(m));
					} else if (rs instanceof Enum<?> m) {
						context.connector.send(S7SMsg.of(context.request).pack(m));
					}

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
		type = S7SMsg.getPayloadType(method);
	}

}
