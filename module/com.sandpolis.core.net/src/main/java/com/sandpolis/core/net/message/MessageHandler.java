//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.message;

import com.google.protobuf.MessageLite;

/**
 * A generic message handler.
 *
 * @param <E> The incoming {@link MessageLite} type
 */
@FunctionalInterface
public interface MessageHandler<E extends MessageLite> {

	/**
	 * React to a message.
	 *
	 * @param message A {@link MessageLite}
	 * @throws Exception
	 */
	public void handle(E message) throws Exception;
}
