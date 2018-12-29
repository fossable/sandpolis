/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.net.loop;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.init.ChannelConstant;
import com.sandpolis.core.net.store.connection.ConnectionStore;
import com.sandpolis.core.proto.util.Generator.LoopConfig;
import com.sandpolis.core.proto.util.Generator.NetworkTarget;

import io.netty.bootstrap.Bootstrap;

/**
 * A {@link ConnectionLoop} is a {@link Thread} which makes repeated connection
 * attempts to a list of {@link NetworkTarget}s until a connection is made or
 * the maximum iteration count has been reached.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ConnectionLoop extends Thread {

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
	 * The {@link Sock} produced by a successful connection attempt, otherwise
	 * {@code null}.
	 */
	private Sock result;

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
	}

	@Override
	public void run() {
		List<NetworkTarget> targets = config.getTargetList();

		try {
			while (!Thread.interrupted()) {

				int timeout = cycler.nextTimeout() / targets.size();
				for (NetworkTarget n : targets) {

					long time = System.currentTimeMillis();
					SockFuture future = new SockFuture(bootstrap.remoteAddress(n.getAddress(), n.getPort()).connect());

					try {
						result = future.get(timeout, TimeUnit.MILLISECONDS);
					} catch (TimeoutException | ExecutionException e) {
						log.trace("Connection attempt timed out after {} ms", timeout);
						result = null;
					}

					if (result != null) {
						// TODO move somewhere else
						ConnectionStore.add(result);
						return;
					}

					time = System.currentTimeMillis() - time;
					if (time < timeout)
						Thread.sleep(timeout - time);
				}

				if (cycler.getIterations() == config.getMaxIterations())
					return;
			}
		} catch (InterruptedException e) {
			return;
		}
	}

	/**
	 * Wait for the {@link ConnectionLoop} to complete.
	 * 
	 * @throws InterruptedException
	 */
	public void await() throws InterruptedException {
		this.join();
	}

	/**
	 * Get the resultant {@link Sock} of this {@code ConnectionLoop}.
	 * 
	 * @return The connection produced by this {@code ConnectionLoop}
	 */
	public Sock getResult() {
		return result;
	}

	/**
	 * Get the {@link LoopConfig} of this {@code ConnectionLoop}.
	 * 
	 * @return The immutable configuration of this {@code ConnectionLoop}
	 */
	public LoopConfig getConfig() {
		return config;
	}

}
