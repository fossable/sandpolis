//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.listener;

import static org.s7s.core.instance.network.NetworkStore.NetworkStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.foundation.S7STcpService;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ServerOid.ListenerOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;
import org.s7s.core.instance.util.ChannelUtil;
import org.s7s.core.server.channel.ServerChannelInitializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

/**
 * A network listener that binds to a port and handles new connections.
 *
 * @since 1.0.0
 */
public class Listener extends AbstractSTDomainObject {

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

	Listener(STDocument document) {
		super(document);
	}

	public void start() {
		if (acceptor != null)
			throw new IllegalStateException("The listener is already running");

		S7STcpService.of(get(ListenerOid.PORT).asInt()).serviceName().ifPresentOrElse(name -> {
			log.debug("Starting listener on port: {} ({})", get(ListenerOid.PORT), name);
		}, () -> {
			log.debug("Starting listener on port: {}", get(ListenerOid.PORT));
		});

		// Build new loop groups to handle socket events
		parentLoopGroup = ChannelUtil.newEventLoopGroup();
		childLoopGroup = ChannelUtil.newEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap()
				// Set the event loop groups
				.group(parentLoopGroup, childLoopGroup)
				// Set the channel class
				.channel(ChannelUtil.getServerChannelType())
				// Set the number of sockets in the backlog
				.option(ChannelOption.SO_BACKLOG, 128)
				// Set the keep-alive option
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		b.childHandler(new ServerChannelInitializer(config -> {
			config.sid = NetworkStore.sid();

			if (get(ListenerOid.CERTIFICATE).isPresent() && get(ListenerOid.PRIVATE_KEY).isPresent()) {
				config.serverTlsWithCert(get(ListenerOid.CERTIFICATE).asBytes(),
						get(ListenerOid.PRIVATE_KEY).asBytes());
			} else {
				config.serverTlsSelfSigned();
			}
		}));

		try {
			acceptor = (ServerChannel) b.bind(get(ListenerOid.ADDRESS).asString(), get(ListenerOid.PORT).asInt())
					.await().channel();
		} catch (InterruptedException e) {
			log.error("Failed to start the listener", e);
			acceptor = null;
			set(ListenerOid.ACTIVE, false);
		}
		set(ListenerOid.ACTIVE, true);
	}

	/**
	 * Stop the listener, leaving all spawned {@link Channel}s alive.
	 */
	public void stop() {
		if (acceptor == null)
			throw new IllegalStateException("The listener is not running");

		log.debug("Stopping listener on port: {}", get(ListenerOid.PORT).asInt());

		try {
			acceptor.close().sync();
		} catch (InterruptedException e) {
			// Ignore
		} finally {
			parentLoopGroup.shutdownGracefully();
			acceptor = null;
			set(ListenerOid.ACTIVE, false);
		}
	}

}
