//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.connection;

import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;

import org.s7s.core.instance.channel.ChannelConstant;

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
			var connection = future.channel().attr(ChannelConstant.SOCK).get();

			if (!future.isSuccess()) {
				ConnectionStore.removeValue(connection);
				ConnectionFuture.this.setFailure(future.cause());
				return;
			}

			future.channel().attr(ChannelConstant.HANDSHAKE_FUTURE).get().addListener(handshakeFuture -> {
				if (handshakeFuture.isSuccess()) {
					ConnectionFuture.this.setSuccess(connection);
				} else {
					ConnectionStore.removeValue(connection);
					ConnectionFuture.this.setFailure(handshakeFuture.cause());
				}
			});
		});
	}
}
