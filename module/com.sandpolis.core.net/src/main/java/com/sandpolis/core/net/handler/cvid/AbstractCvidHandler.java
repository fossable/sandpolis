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
package com.sandpolis.core.net.handler.cvid;

import com.sandpolis.core.proto.net.Message.MSG;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public abstract class AbstractCvidHandler extends SimpleChannelInboundHandler<MSG> {

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
