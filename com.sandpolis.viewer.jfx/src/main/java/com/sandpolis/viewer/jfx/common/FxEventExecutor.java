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
package com.sandpolis.viewer.jfx.common;

import java.util.concurrent.TimeUnit;

import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import javafx.application.Platform;

/**
 * This {@link EventExecutor} implementation runs tasks on the JavaFX
 * application thread. The primary motivation is to allow {@link Future}
 * listeners to be executed on the GUI update thread.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class FxEventExecutor extends AbstractEventExecutor {

	private boolean shutdown = false;

	@Override
	public boolean isShuttingDown() {
		return shutdown;
	}

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public boolean isTerminated() {
		return shutdown;
	}

	@Override
	public void shutdown() {
		shutdown = true;
		// Nothing to clean up
	}

	@Override
	public boolean inEventLoop(Thread thread) {
		if (!Thread.currentThread().equals(thread))
			throw new UnsupportedOperationException(
					"FxEventExecutor supports querying the eventloop status of the current thread only");

		return Platform.isFxApplicationThread();
	}

	@Override
	public void execute(Runnable command) {
		Platform.runLater(command);
	}

	@Override
	public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<?> terminationFuture() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

}
