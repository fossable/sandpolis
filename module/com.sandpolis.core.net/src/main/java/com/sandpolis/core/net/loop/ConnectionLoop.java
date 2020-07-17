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
package com.sandpolis.core.net.loop;

import static com.google.common.base.Preconditions.checkArgument;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Generator.LoopConfig;
import com.sandpolis.core.instance.Generator.NetworkTarget;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.connection.ConnectionFuture;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.EventExecutor;

/**
 * A {@link ConnectionLoop} makes repeated connection attempts to a list of
 * {@link NetworkTarget}s until a connection is made or the maximum iteration
 * count has been reached. The connection attempt interval can be configured to
 * slowly ease up on a host that is consistently refusing connections.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ConnectionLoop implements Runnable {

	public static final Logger log = LoggerFactory.getLogger(ConnectionLoop.class);

	/**
	 * The loop configuration.
	 */
	private final LoopConfig config;

	/**
	 * The {@link Bootstrap} which will be used for each connection attempt.
	 */
	private final Bootstrap bootstrap;

	/**
	 * The {@link ConnectionFuture} that will be notified by a successful connection
	 * attempt or when the maximum iteration count is reached.
	 */
	private final ConnectionFuture future;

	/**
	 * The exponential function that calculates the connection cooldown.
	 */
	private final Supplier<Integer> exponential;

	/**
	 * The current connection cooldown.
	 */
	private int cooldown;

	/**
	 * The current iteration ordinal.
	 */
	private int iteration;

	/**
	 * Create a new single-iteration {@link ConnectionLoop}.
	 *
	 * @param address The target address
	 * @param port    The target port
	 * @param timeout The connection timeout in milliseconds
	 */
	public ConnectionLoop(String address, int port, int timeout, Bootstrap bootstrap) {
		this(LoopConfig.newBuilder().addTarget(NetworkTarget.newBuilder().setAddress(address).setPort(port))
				.setTimeout(timeout).setIterationLimit(1), bootstrap);
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
		checkArgument(config.getIterationLimit() >= 0);
		checkArgument(config.getCooldown() >= 0);
		checkArgument(config.getTimeout() > 0);

		this.config = Objects.requireNonNull(config);
		this.bootstrap = Objects.requireNonNull(bootstrap);

		// Set channel options
		bootstrap.attr(ChannelConstant.STRICT_CERTS, config.getStrictCerts());
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getTimeout());

		// Build a SockFuture without a ChannelFuture
		this.future = new ConnectionFuture((EventExecutor) ThreadStore.get("net.connection.loop"));

		// Setup cooldown supplier
		if (config.getCooldownConstant() == 0 || config.getCooldownLimit() <= config.getCooldown()) {
			this.exponential = config::getCooldown;
		} else {
			this.exponential = () -> {
				return (int) Math.min(config.getCooldownLimit(), config.getCooldown()
						* Math.pow(config.getCooldown(), iteration / config.getCooldownConstant()));
			};
		}
		this.cooldown = config.getCooldown();
	}

	@Override
	public void run() {

		try {
			while (iteration < config.getIterationLimit() || config.getIterationLimit() == 0) {

				for (NetworkTarget n : config.getTargetList()) {

					ConnectionFuture connect = new ConnectionFuture(bootstrap.remoteAddress(n.getAddress(), n.getPort()).connect());
					try {
						connect.sync();
					} catch (Exception e) {
						log.debug("Attempt failed: {}", e.getMessage());
					}

					if (connect.isSuccess()) {
						this.future.setSuccess(connect.get());
						return;
					}

					iteration++;
					cooldown = exponential.get();
					Thread.sleep(cooldown);
				}
			}

			// Maximum iteration count exceeded
			future.setSuccess(null);
		} catch (Exception e) {
			future.setFailure(e);
		}
	}

	/**
	 * Start the {@link ConnectionLoop}.
	 *
	 * @return A {@link ConnectionFuture}
	 */
	public ConnectionFuture start() {
		return start((ExecutorService) ThreadStore.get("net.connection.loop"));
	}

	/**
	 * Start the {@link ConnectionLoop}.
	 *
	 * @param executor The executor service
	 * @return A {@link ConnectionFuture}
	 */
	public ConnectionFuture start(ExecutorService executor) {
		executor.execute(this);
		return future;
	}

	/**
	 * Get the loop's {@link ConnectionFuture}.
	 *
	 * @return The connection future
	 */
	public ConnectionFuture future() {
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
