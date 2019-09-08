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
