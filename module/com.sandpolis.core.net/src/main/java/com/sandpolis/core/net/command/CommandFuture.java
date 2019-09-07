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
package com.sandpolis.core.net.command;

import com.google.protobuf.Message;
import com.sandpolis.core.proto.util.Result.Outcome;

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
