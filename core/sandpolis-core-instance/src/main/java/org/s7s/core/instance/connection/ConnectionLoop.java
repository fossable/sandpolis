//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.connection;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.protocol.Channel.ChannelTransportProtocol;
import org.s7s.core.instance.util.ChannelUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.EventExecutor;

/**
 * A {@link ConnectionLoop} makes repeated connection attempts to a set of
 * targets until either a connection is made or the maximum iteration count has
 * been reached. The connection attempt interval can be configured to slowly
 * ease up on a host that is consistently refusing connections (exponential
 * cooldown).
 *
 * @since 0.1.0
 */
public final class ConnectionLoop implements Runnable {

	public static final class ConfigStruct {

		/**
		 * The Netty {@link Bootstrap} to use for the connection attempt.
		 */
		public final Bootstrap bootstrap = new Bootstrap();

		/**
		 * The amount of time to wait after each failed attempt in milliseconds.
		 */
		public int cooldown = 5000;

		/**
		 * The exponential cooldown time constant which is the number of iterations
		 * required for the total cooldown to increase by a factor of the initial
		 * cooldown. A value of 0 disables exponential cooldown.
		 */
		public double cooldownConstant = 0.0;

		/**
		 * The maximum cooldown in milliseconds. This value is only applicable when
		 * exponential cooldown is enabled. A value of 0 implies there is no limit.
		 */
		public int cooldownLimit = 0;

		/**
		 * The maximum number of attempts to make before giving up.
		 */
		public int iterationLimit = 0;

		/**
		 * A set of targets that will be tried sequentially.
		 */
		public final List<Target> targets = new ArrayList<>();

		/**
		 * The connection timeout in milliseconds.
		 */
		public int timeout = 1000;

		public void address(String address) {
			if (address.contains(":")) {
				var components = address.split(":");
				address(components[0], Integer.parseInt(components[1]));
			} else {
				address(address, 8768);
			}
		}

		public void address(String address, int port) {
			targets.add(new Target(address, port));
		}

		private ConfigStruct(Consumer<ConfigStruct> configurator) {
			configurator.accept(this);

			if (timeout <= 0) {
				throw new RuntimeException("Invalid timeout: " + timeout);
			}

			if (cooldown < 0) {
				throw new RuntimeException("Invalid cooldown: " + cooldown);
			}

			if (iterationLimit < 0) {
				throw new RuntimeException("Invalid iterationLimit: " + iterationLimit);
			}

			if (targets.size() == 0) {
				throw new RuntimeException("No targets specified");
			}
		}
	}

	/**
	 * Represents an IP address or DNS name and a port.
	 */
	public static record Target(String address, int port) {
	}

	public static final Logger log = LoggerFactory.getLogger(ConnectionLoop.class);

	private final Bootstrap bootstrap;

	private int cooldown;

	private final double cooldownConstant;

	private final int cooldownLimit;

	/**
	 * The exponential function that calculates the connection cooldown.
	 */
	private final Supplier<Integer> exponential;

	/**
	 * The {@link ConnectionFuture} that will be notified by a successful connection
	 * attempt or when the maximum iteration count is reached.
	 */
	private final ConnectionFuture future;

	private int iteration;

	private final int iterationLimit;

	private final List<Target> targets;

	public ConnectionLoop(Consumer<ConfigStruct> configurator) {
		var config = new ConfigStruct(configurator);

		this.bootstrap = config.bootstrap;
		this.targets = config.targets;
		this.cooldown = config.cooldown;
		this.cooldownConstant = config.cooldownConstant;
		this.cooldownLimit = config.cooldownLimit;
		this.iterationLimit = config.iterationLimit;

		// Set channel options
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.timeout);

		if (bootstrap.config().group() == null) {
			bootstrap.group(ThreadStore.get("net.connection.outgoing"));
		}

		// Set default channel factory
		if (bootstrap.config().channelFactory() == null) {
			bootstrap.channel(ChannelUtil.getChannelType(ChannelTransportProtocol.TCP));
		}

		// Build a SockFuture without a ChannelFuture
		this.future = new ConnectionFuture((EventExecutor) ThreadStore.get("net.connection.loop"));

		// Setup cooldown supplier
		if (cooldownConstant == 0 || cooldownLimit <= config.cooldown) {
			this.exponential = () -> cooldown;
		} else {
			this.exponential = () -> {
				return (int) Math.min(cooldownLimit,
						config.cooldown * Math.pow(config.cooldown, iteration / cooldownConstant));
			};
		}
		this.cooldown = config.cooldown;
	}

	/**
	 * Get the loop's {@link ConnectionFuture}.
	 *
	 * @return The connection future
	 */
	public ConnectionFuture future() {
		return future;
	}

	@Override
	public void run() {

		log.debug("Starting connection loop (target count = {}, iteration limit = {}, cooldown = {})", targets.size(),
				iterationLimit, cooldown);

		try {
			while (iteration < iterationLimit || iterationLimit == 0) {

				if (iteration > 0) {
					log.trace("Waiting {} ms before next connection attempt", cooldown);
					Thread.sleep(cooldown);
				}

				for (var target : targets) {

					log.debug("Attempting connection to {} on port {}", target.address(), target.port());
					var connect = new ConnectionFuture(
							bootstrap.remoteAddress(target.address(), target.port()).connect()).await();

					if (connect.isSuccess()) {
						log.debug("Connection attempt succeeded");
						future.setSuccess(connect.get());
						return;
					} else {
						log.debug("Connection attempt failed");
					}

					iteration++;
					cooldown = exponential.get();
				}
			}

			// Maximum iteration count exceeded
			log.debug("Maximum connection iteration count exceeded");
			future.setFailure(new Exception("Maximum connection iteration count exceeded"));
		} catch (Exception e) {
			log.debug("Encountered exception in connection loop", e);
			future.setFailure(e);
		}
	}

	/**
	 * Begin the connection process.
	 *
	 * @return A future which will receive the connection if successful
	 */
	public ConnectionLoop start() {
		return start((ExecutorService) ThreadStore.get("net.connection.loop"));
	}

	/**
	 * Begin the connection process on the given {@link ExecutorService}.
	 *
	 * @param executor The executor service
	 * @return A future which will receive the connection if successful
	 */
	public ConnectionLoop start(ExecutorService executor) {
		executor.execute(this);
		return this;
	}

}
