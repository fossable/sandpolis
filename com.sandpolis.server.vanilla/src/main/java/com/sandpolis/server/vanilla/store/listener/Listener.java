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
package com.sandpolis.server.vanilla.store.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.DocumentBindings.Profile;
import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.instance.data.Document;
import com.sandpolis.core.net.Transport;
import com.sandpolis.core.foundation.util.NetUtil;
import com.sandpolis.server.vanilla.net.init.ServerChannelInitializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

/**
 * A network listener that binds to a port and handles new connections.
 *
 * @author cilki
 * @since 1.0.0
 */
public class Listener extends Profile.Instance.Server.Listener {

	public static final Logger log = LoggerFactory.getLogger(Listener.class);

	/**
	 * The listening {@link Channel} that is bound to the listening network
	 * interface.
	 */
	private ServerChannel acceptor;

	/**
	 * The {@link EventLoopGroup} that handles the {@link ServerChannel}.
	 */
	private EventLoopGroup parentLoopGroup;

	/**
	 * The {@link EventLoopGroup} that handles all spawned {@link Channel}s.
	 */
	private EventLoopGroup childLoopGroup;

	/**
	 * Construct a new {@link Listener} from a configuration.
	 *
	 * @param config The configuration which should be prevalidated and complete
	 */
	public Listener(Document document, ListenerConfig config) {
		super(document);

		port().set(config.getPort());
	}

	/**
	 * Start the listener.
	 */
	public void start() {
		if (acceptor != null)
			throw new IllegalStateException("The listener is already running");

		NetUtil.serviceName(getPort()).ifPresentOrElse(name -> {
			log.debug("Starting listener on port: {} ({})", getPort(), name);
		}, () -> {
			log.debug("Starting listener on port: {}", getPort());
		});

		// Build new loop groups to handle socket events
		parentLoopGroup = Transport.INSTANCE.getEventLoopGroup();
		childLoopGroup = Transport.INSTANCE.getEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap()
				// Set the event loop groups
				.group(parentLoopGroup, childLoopGroup)
				// Set the channel class
				.channel(Transport.INSTANCE.getServerSocketChannel())
				// Set the number of sockets in the backlog
				.option(ChannelOption.SO_BACKLOG, 128)
				// Set the keep-alive option
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		if (certificate().isPresent() && privateKey().isPresent())
			b.childHandler(new ServerChannelInitializer(Core.cvid(), getCertificate(), getPrivateKey()));
		else
			b.childHandler(new ServerChannelInitializer(Core.cvid()));

		try {
			acceptor = (ServerChannel) b.bind(getAddress(), getPort()).await().channel();
		} catch (InterruptedException e) {
			log.error("Failed to start the listener", e);
			acceptor = null;
			active().set(false);
		}
		active().set(true);
	}

	/**
	 * Stop the listener, leaving all spawned {@link Channel}s alive.
	 */
	public void stop() {
		if (acceptor == null)
			throw new IllegalStateException("The listener is not running");

		log.debug("Stopping listener on port: {}", getPort());

		try {
			acceptor.close().sync();
		} catch (InterruptedException e) {
			// Ignore
		} finally {
			parentLoopGroup.shutdownGracefully();
			acceptor = null;
			active().set(false);
		}
	}
}
