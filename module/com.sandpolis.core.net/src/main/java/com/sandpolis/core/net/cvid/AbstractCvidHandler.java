//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.cvid;

import com.sandpolis.core.net.Message.MSG;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public abstract class AbstractCvidHandler extends SimpleChannelInboundHandler<MSG> {

	/**
	 * This event is emitted by both {@link CvidRequestHandler} and
	 * {@link CvidResponseHandler} on the completion of the CVID handshake.
	 */
	public static final class CvidHandshakeCompletionEvent {

		/**
		 * Whether the handshake was successful.
		 */
		public final boolean success;

		/**
		 * The local CVID if the handshake was successful.
		 */
		public final int local;

		/**
		 * The remote CVID if the handshake was successful.
		 */
		public final int remote;

		public CvidHandshakeCompletionEvent(int local, int remote) {
			this.success = true;
			this.local = local;
			this.remote = remote;
		}

		public CvidHandshakeCompletionEvent() {
			this.success = false;
			this.local = 0;
			this.remote = 0;
		}
	}
}
