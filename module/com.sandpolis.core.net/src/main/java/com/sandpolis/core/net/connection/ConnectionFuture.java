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
package com.sandpolis.core.net.connection;

import com.sandpolis.core.net.ChannelConstant;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

/**
 * A {@link ConnectionFuture} provides an easy way to asynchronously obtain a
 * configured {@link Connection}.
 *
 * @author cilki
 * @since 5.0.0
 */
public class ConnectionFuture extends DefaultPromise<Connection> {

	/**
	 * Construct a new {@link ConnectionFuture} that must be completed externally.
	 *
	 * @param executor A custom executor
	 */
	public ConnectionFuture(EventExecutor executor) {
		super(executor);
	}

	/**
	 * Construct a new {@link ConnectionFuture} which will provide a new
	 * {@link Connection} once the given connection is established.
	 *
	 * @param connect A connection future
	 */
	public ConnectionFuture(ChannelFuture connect) {
		this(connect.channel().eventLoop(), connect);
	}

	/**
	 * Construct a new {@link ConnectionFuture} which will provide a new
	 * {@link Connection} once the given connection is established.
	 *
	 * @param executor A custom executor
	 * @param connect  A connection future
	 */
	public ConnectionFuture(EventExecutor executor, ChannelFuture connect) {
		super(executor);

		connect.addListener((ChannelFuture future) -> {
			if (!future.isSuccess()) {
				ConnectionFuture.this.setFailure(future.cause());
				return;
			}

			future.channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get().addListener(handshakeFuture -> {
				if (handshakeFuture.isSuccess()) {
					ConnectionFuture.this.setSuccess(future.channel().attr(ChannelConstant.SOCK).get());
				} else {
					ConnectionFuture.this.setFailure(handshakeFuture.cause());
				}
			});
		});
	}
}
