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
package com.sandpolis.core.server.listener;

import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_ADDRESS;
import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_CERTIFICATE;
import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_KEY;
import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_PORT;
import static com.sandpolis.core.foundation.Result.ErrorCode.OK;
import static com.sandpolis.core.foundation.util.ValidationUtil.ipv4;
import static com.sandpolis.core.foundation.util.ValidationUtil.port;

import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.Result.ErrorCode;
import com.sandpolis.core.foundation.util.CertUtil;
import com.sandpolis.core.foundation.util.NetUtil;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.StateTree.VirtProfile.VirtServer.VirtListener;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.state.StateObject;
import com.sandpolis.core.net.util.ChannelUtil;
import com.sandpolis.core.server.net.init.ServerChannelInitializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * A network listener that binds to a port and handles new connections.
 *
 * @since 1.0.0
 */
public class Listener extends VirtListener {

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

	Listener(Document document) {
		super(document);
	}

	public void start() {
		if (acceptor != null)
			throw new IllegalStateException("The listener is already running");

		NetUtil.serviceName(getPort()).ifPresentOrElse(name -> {
			log.debug("Starting listener on port: {} ({})", getPort(), name);
		}, () -> {
			log.debug("Starting listener on port: {}", getPort());
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
			config.cvid = Core.cvid();
			try {
				config.tls = SslContextBuilder
						.forServer(CertUtil.parseKey(getPrivateKey()), CertUtil.parseCert(getCertificate()))
						.protocols("TLSv1.3");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));

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

	@Override
	public ErrorCode valid() {

		if (port().isPresent() && !ValidationUtil.port(getPort()))
			return INVALID_PORT;
		if (address().isPresent() && !ipv4(getAddress()))
			return INVALID_ADDRESS;
		if (!certificate().isPresent() && privateKey().isPresent())
			return INVALID_CERTIFICATE;
		if (certificate().isPresent() && !privateKey().isPresent())
			return INVALID_KEY;
		if (certificate().isPresent() && privateKey().isPresent()) {
			// Check certificate and key formats
			try {
				CertUtil.parseCert(getCertificate());
			} catch (CertificateException e) {
				return INVALID_CERTIFICATE;
			}

			try {
				CertUtil.parseKey(getPrivateKey());
			} catch (InvalidKeySpecException e) {
				return INVALID_KEY;
			}
		}

		return OK;
	}

	@Override
	public ErrorCode complete() {

		if (!port().isPresent())
			return INVALID_PORT;
		if (!address().isPresent())
			return INVALID_ADDRESS;

		return OK;
	}
}
