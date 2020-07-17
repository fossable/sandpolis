package com.sandpolis.core.net.message;

import com.google.protobuf.Message;

/**
 * A generic message handler.
 *
 * @param <E> The incoming {@link Message} type
 */
@FunctionalInterface
public interface MessageHandler<E extends Message> {

	/**
	 * React to a message.
	 *
	 * @param message A {@link Message}
	 * @throws Exception
	 */
	public void handle(E message) throws Exception;
}
