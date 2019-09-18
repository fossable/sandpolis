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
package com.sandpolis.core.net.handler.cvid;

import com.sandpolis.core.proto.net.MSG.Message;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public abstract class AbstractCvidHandler extends SimpleChannelInboundHandler<Message> {

	/**
	 * This event is emitted by both {@link CvidRequestHandler} and
	 * {@link CvidResponseHandler} on the completion of the CVID handshake.
	 */
	public static final class CvidHandshakeCompletionEvent {

		private final boolean success;
		private final int cvid;

		public CvidHandshakeCompletionEvent(int cvid) {
			this.success = true;
			this.cvid = cvid;
		}

		public CvidHandshakeCompletionEvent() {
			this.success = false;
			this.cvid = 0;
		}

		public boolean isSuccess() {
			return success;
		}

		public int getCvid() {
			return cvid;
		}
	}
}
