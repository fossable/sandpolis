//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.message;

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
