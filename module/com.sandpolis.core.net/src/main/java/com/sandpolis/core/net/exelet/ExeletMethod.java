package com.sandpolis.core.net.exelet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.util.MsgUtil;

public class ExeletMethod {

	private static final Logger log = LoggerFactory.getLogger(ExeletMethod.class);

	public final Exelet.Handler metadata;

	public final String url;

	private Consumer<ExeletContext> handler;

	/**
	 * Consume an {@link ExeletContext}.
	 * 
	 * @param context The context which wraps a request
	 */
	public void accept(ExeletContext context) {
		handler.accept(context);
	}

	public ExeletMethod(Method method) throws IllegalAccessException {
		metadata = checkNotNull(method.getAnnotation(Exelet.Handler.class), "Method not a handler");

		// Obtain a handle to the static method
		var handle = MethodHandles.publicLookup().unreflect(method);

		// TYPE 2
		if (method.getReturnType() == MessageOrBuilder.class && method.getParameterCount() == 1
				&& Message.class.isAssignableFrom(method.getParameterTypes()[0])) {

			handler = context -> {
				// Access control
				if (!context.connector.isAuthenticated() && metadata.auth()) {
					return;
				}

				try {
					MessageOrBuilder rs = (MessageOrBuilder) handle
							.invoke(context.request.getPayload().unpack((Class) method.getParameterTypes()[0]));

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
				&& Message.class.isAssignableFrom(method.getParameterTypes()[0])) {

			handler = context -> {
				// Access control
				if (!context.connector.isAuthenticated() && metadata.auth()) {
					return;
				}

				try {
					handle.invoke(context.request.getPayload().unpack((Class) method.getParameterTypes()[0]));
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

			handler = context -> {
				// Access control
				if (!context.connector.isAuthenticated() && metadata.auth()) {
					return;
				}

				try {
					handle.invoke(context, context.request.getPayload().unpack((Class) method.getParameterTypes()[1]));
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
		else if (method.getReturnType() == MessageOrBuilder.class && method.getParameterCount() == 2
				&& method.getParameterTypes()[0] == ExeletContext.class
				&& Message.class.isAssignableFrom(method.getParameterTypes()[1])) {

			handler = context -> {
				// Access control
				if (!context.connector.isAuthenticated() && metadata.auth()) {
					return;
				}

				try {
					MessageOrBuilder rs = (MessageOrBuilder) handle.invoke(context,
							context.request.getPayload().unpack((Class) method.getParameterTypes()[1]));
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
			throw new RuntimeException("Unknown handler format for method: " + method.getName());

		// Set this last in case the method was not a valid handler
		url = MsgUtil.getTypeUrl(method);
	}

}
