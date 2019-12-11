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
package com.sandpolis.core.net.exception;

import com.sandpolis.core.proto.net.Message.MSG;

/**
 * Thrown when the message flow is disrupted. This exception occurs when: <br>
 * <ul>
 * <li>A received message is of an unexpected type</li>
 * </ul>
 *
 * @author cilki
 * @since 4.0.0
 */
public class MessageFlowException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Construct a new {@link MessageFlowException}.
	 *
	 * @param sent     The sent message's type
	 * @param received The received message
	 * @param expected The expected response type
	 */
	public MessageFlowException(Class<?> sent, MSG received, Class<?> expected) {
		super(String.format("After sending a %s message, an unexpected %s was received. Expected: %s.",
				sent.getClass().getSimpleName(), received.getPayloadCase(), expected.getClass().getSimpleName()));
	}

	/**
	 * Construct a new {@link MessageFlowException}.
	 *
	 * @param sent     The sent message's type
	 * @param received The received message
	 */
	public MessageFlowException(Class<?> sent, MSG received) {
		super(String.format("After sending a %s message, an unexpected %s was received.",
				sent.getClass().getSimpleName(), received.getPayloadCase()));
	}

}
