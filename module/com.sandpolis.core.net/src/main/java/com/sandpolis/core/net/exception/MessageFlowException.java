/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.net.exception;

import com.sandpolis.core.proto.net.MSG.Message;

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
	public MessageFlowException(Class<?> sent, Message received, Class<?> expected) {
		super(String.format("After sending a %s message, an unexpected %s was received. Expected: %s.",
				sent.getClass().getSimpleName(), received.getMsgOneofCase(), expected.getClass().getSimpleName()));
	}

	/**
	 * Construct a new {@link MessageFlowException}.
	 *
	 * @param sent     The sent message's type
	 * @param received The received message
	 */
	public MessageFlowException(Class<?> sent, Message received) {
		super(String.format("After sending a %s message, an unexpected %s was received.",
				sent.getClass().getSimpleName(), received.getMsgOneofCase()));
	}

}
