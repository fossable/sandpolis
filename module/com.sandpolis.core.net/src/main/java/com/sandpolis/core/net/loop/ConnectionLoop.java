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
package com.sandpolis.core.net.loop;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.Sock.ConnectionState;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.proto.util.Generator.LoopConfig;
import com.sandpolis.core.proto.util.Generator.NetworkTarget;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.EventExecutor;

/**
 * A {@link ConnectionLoop} makes repeated connection attempts to a list of
 * {@link NetworkTarget}s until a connection is made or the maximum iteration
 * count has been reached. The connection attempt interval can be configured to
 * slowly back off.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ConnectionLoop implements Runnable {

	public static final Logger log = LoggerFactory.getLogger(ConnectionLoop.class);

	/**
	 * The loop configuration.
	 */
	private LoopConfig config;

	/**
	 * The loop cycler which determines the wait interval.
	 */
	private LoopCycle cycler;

	/**
	 * The {@link Bootstrap} which will be used for each connection attempt.
	 */
	private Bootstrap bootstrap;

	/**
	 * The {@link SockFuture} that will be notified by a successful connection
	 * attempt.
	 */
	private SockFuture future;

	/**
	 * Create a new single-iteration {@link ConnectionLoop}.
	 *
	 * @param address The target address
	 * @param port    The target port
	 * @param timeout The connection timeout in milliseconds
	 */
	public ConnectionLoop(String address, int port, int timeout, Bootstrap bootstrap) {
		this(LoopConfig.newBuilder().addTarget(NetworkTarget.newBuilder().setAddress(address).setPort(port))
				.setTimeout(timeout).setMaxTimeout(timeout).setMaxIterations(1), bootstrap);
	}

	/**
	 * Create a new {@link ConnectionLoop}.
	 *
	 * @param config The configuration object
	 */
	public ConnectionLoop(LoopConfig.Builder config, Bootstrap bootstrap) {
		this(config.build(), bootstrap);
	}

	/**
	 * Create a new {@link ConnectionLoop}.
	 *
	 * @param config The configuration object
	 */
	public ConnectionLoop(LoopConfig config, Bootstrap bootstrap) {
		this.config = Objects.requireNonNull(config);
		this.bootstrap = Objects.requireNonNull(bootstrap);

		if (config.getTimeoutFlatness() == 0)
			this.cycler = new LoopCycle(config.getTimeout(), config.getMaxTimeout());
		else
			this.cycler = new LoopCycle(config.getTimeout(), config.getMaxTimeout(), config.getTimeoutFlatness());

		bootstrap.attr(ChannelConstant.STRICT_CERTS, config.getStrictCerts());

		future = new SockFuture((EventExecutor) ThreadStore.get("temploop"));
	}

	@Override
	public void run() {

		try {
			while (cycler.getIterations() < config.getMaxIterations() || config.getMaxIterations() == 0) {

				int timeout = cycler.nextTimeout() / config.getTargetCount();
				for (NetworkTarget n : config.getTargetList()) {
					long time = System.currentTimeMillis();

					SockFuture future = new SockFuture(bootstrap.remoteAddress(n.getAddress(), n.getPort()).connect());
					future.await(timeout, TimeUnit.MILLISECONDS);

					Sock sock = future.getNow();
					if (future.isSuccess() && sock != null) {
						// Check connection state
						if (sock.getState() == ConnectionState.CONNECTED) {
							this.future.setSuccess(sock);
							return;
						}
					}

					future.cancel(true);

					time = System.currentTimeMillis() - time;
					if (time < timeout)
						Thread.sleep(timeout - time);
				}
			}

			// Maximum iteration count exceeded
			future.setSuccess(null);
		} catch (InterruptedException e) {
			future.setFailure(e);
		} catch (Exception e) {
			future.setFailure(e);
		}
	}

	/**
	 * Start the {@link ConnectionLoop}.
	 *
	 * @return A {@link SockFuture}
	 */
	public SockFuture start() {
		return start((ExecutorService) ThreadStore.get("temploop"));
	}

	/**
	 * Start the {@link ConnectionLoop}.
	 *
	 * @param executor The executor service
	 * @return A {@link SockFuture}
	 */
	public SockFuture start(ExecutorService executor) {
		executor.execute(this);
		return future;
	}

	/**
	 * Get the loop's {@link SockFuture}.
	 *
	 * @return The connection future
	 */
	public SockFuture future() {
		return future;
	}

	/**
	 * Get the loop's {@link LoopConfig}.
	 *
	 * @return The immutable configuration of this {@link ConnectionLoop}
	 */
	public LoopConfig getConfig() {
		return config;
	}

}
