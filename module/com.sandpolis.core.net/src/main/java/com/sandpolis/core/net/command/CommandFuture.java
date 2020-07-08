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
package com.sandpolis.core.net.command;

import com.google.protobuf.Message;
import com.sandpolis.core.foundation.Result.Outcome;

import io.netty.util.concurrent.Future;

/**
 * A {@link Future} that represents the completion of a {@link Cmdlet} command.
 *
 * @author cilki
 * @since 5.0.0
 */
public interface CommandFuture extends Future<Outcome> {

	/**
	 * A generic message handler.
	 *
	 * @param <E> The incoming {@link Message} type
	 */
	@FunctionalInterface
	public static interface MessageHandler<E extends Message> {

		/**
		 * React to a message.
		 *
		 * @param message A {@link Message}
		 * @throws Exception
		 */
		public void handle(E message) throws Exception;
	}
}
