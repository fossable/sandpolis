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

import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_ADDRESS;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_CERTIFICATE;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_KEY;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_PORT;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.instance.Result.ErrorCode.OK;
import static com.sandpolis.core.util.ValidationUtil.ipv4;
import static com.sandpolis.core.util.ValidationUtil.port;
import static com.sandpolis.core.util.ValidationUtil.username;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Listener.ProtoListener;
import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.instance.Result.ErrorCode;
import com.sandpolis.core.net.Transport;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.core.util.NetUtil;
import com.sandpolis.server.vanilla.net.init.ServerChannelInitializer;
import com.sandpolis.server.vanilla.store.user.User;

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
@Entity
public class Listener implements ProtoType<ProtoListener> {

	public static final Logger log = LoggerFactory.getLogger(Listener.class);

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The unique ID.
	 */
	@Column(nullable = false, unique = true)
	private long id;

	/**
	 * The listener's optional user-friendly name.
	 */
	@Column
	private String name;

	/**
	 * The user that owns the listener.
	 */
	@ManyToOne(optional = false)
	@JoinColumn(referencedColumnName = "db_id")
	private User owner;

	/**
	 * Indicates whether the listener can be started.
	 */
	@Column(nullable = false)
	private boolean enabled;

	/**
	 * The listening port.
	 */
	@Column(nullable = false)
	private int port;

	/**
	 * The listening address.
	 */
	@Column(nullable = false)
	private String address;

	/**
	 * Indicates whether automatic port forwarding with UPnP will be attempted.
	 */
	@Column
	private boolean upnp;

	/**
	 * Indicates whether client instances can be accepted by the listener.
	 */
	@Column
	private boolean clientAcceptor;

	/**
	 * Indicates whether viewer instances can be accepted by the listener.
	 */
	@Column
	private boolean viewerAcceptor;

	/**
	 * The listener's certificate.
	 */
	@Lob
	@Column
	private byte[] certificate;

	/**
	 * The listener's private key.
	 */
	@Lob
	@Column
	private byte[] privateKey;

	/**
	 * The listening {@link Channel} that is bound to the listening network
	 * interface.
	 */
	@Transient
	private ServerChannel acceptor;

	/**
	 * The {@link EventLoopGroup} that handles the {@link ServerChannel}.
	 */
	@Transient
	private EventLoopGroup parentLoopGroup;

	/**
	 * The {@link EventLoopGroup} that handles all spawned {@link Channel}s.
	 */
	@Transient
	private EventLoopGroup childLoopGroup;

	// JPA Constructor
	Listener() {
	}

	/**
	 * Construct a new {@link Listener} from a configuration.
	 *
	 * @param config The configuration which should be prevalidated and complete
	 */
	public Listener(ProtoListener config) {
		if (Listener.valid(config) != ErrorCode.OK)
			throw new IllegalArgumentException();

		merge(config);
	}

	/**
	 * Start the listener.
	 */
	public boolean start() {
		if (isListening())
			throw new IllegalStateException();

		NetUtil.serviceName(port).ifPresentOrElse(name -> {
			log.debug("Starting listener on port: {} ({})", port, name);
		}, () -> {
			log.debug("Starting listener on port: {}", port);
		});

		parentLoopGroup = Transport.INSTANCE.getEventLoopGroup();
		childLoopGroup = Transport.INSTANCE.getEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap();
		b.group(parentLoopGroup, childLoopGroup).channel(Transport.INSTANCE.getServerSocketChannel())//
				.option(ChannelOption.SO_BACKLOG, 128)// Socket backlog
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		if (certificate != null && privateKey != null)
			b.childHandler(new ServerChannelInitializer(Core.cvid(), certificate, privateKey));
		else
			b.childHandler(new ServerChannelInitializer(Core.cvid()));

		try {
			acceptor = (ServerChannel) b.bind(address, port).await().channel();
		} catch (InterruptedException e) {
			log.error("Failed to start the listener", e);
			acceptor = null;
			return false;
		}
		return true;
	}

	/**
	 * Stop the listener, leaving all spawned {@link Channel}s alive.
	 */
	public void stop() {
		if (!isListening())
			throw new IllegalStateException();

		log.debug("Stopping listener: {}", id);

		try {
			acceptor.close().sync();
		} catch (InterruptedException e) {
			// Ignore
		} finally {
			parentLoopGroup.shutdownGracefully();
			acceptor = null;
		}
	}

	/**
	 * Indicates whether the {@link Listener} is currently accepting connections.
	 *
	 * @return True if this listener is currently listening on the socket; false
	 *         otherwise
	 */
	public boolean isListening() {
		return acceptor != null;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isUpnp() {
		return upnp;
	}

	public void setUpnp(boolean upnp) {
		this.upnp = upnp;
	}

	public boolean isClientAcceptor() {
		return clientAcceptor;
	}

	public void setClientAcceptor(boolean clientAcceptor) {
		this.clientAcceptor = clientAcceptor;
	}

	public boolean isViewerAcceptor() {
		return viewerAcceptor;
	}

	public void setViewerAcceptor(boolean viewerAcceptor) {
		this.viewerAcceptor = viewerAcceptor;
	}

	public byte[] getCertificate() {
		return certificate;
	}

	public void setCertificate(byte[] certificate) {
		this.certificate = certificate;
	}

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(byte[] privateKey) {
		this.privateKey = privateKey;
	}

	@Override
	public void merge(ProtoListener config) {

		if (config.hasId())
			this.id = config.getId();
		if (config.hasName())
			setName(config.getName());
		if (config.hasPort())
			setPort(config.getPort());
		if (config.hasAddress())
			setAddress(config.getAddress());
		if (config.hasOwner())
			setOwner(UserStore.get(config.getOwner()).get());
		if (config.hasUpnp())
			setUpnp(config.getUpnp());
		if (config.hasClientAcceptor())
			setClientAcceptor(config.getClientAcceptor());
		if (config.hasViewerAcceptor())
			setViewerAcceptor(config.getViewerAcceptor());
		if (config.hasEnabled())
			setEnabled(config.getEnabled());
		if (config.hasCert())
			setCertificate(config.getCert().toByteArray());
		if (config.hasKey())
			setPrivateKey(config.getKey().toByteArray());
	}

	@Override
	public ProtoListener serialize() {
		ProtoListener.Builder config = ProtoListener.newBuilder().setId(getId()).setName(getName())
				.setOwner(getOwner().getUsername()).setEnabled(isEnabled()).setPort(getPort()).setAddress(getAddress())
				.setUpnp(isUpnp()).setClientAcceptor(isClientAcceptor()).setViewerAcceptor(isViewerAcceptor());
		if (getCertificate() != null && getPrivateKey() != null)
			config.setCert(ByteString.copyFrom(getCertificate())).setKey(ByteString.copyFrom(getPrivateKey()));

		return config.build();
	}

	/**
	 * Validate a {@link ProtoListener}. A {@link ProtoType} is valid if all present
	 * fields pass value restrictions.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public static ErrorCode valid(ProtoListener config) {
		if (config == null)
			throw new IllegalArgumentException();

		if (config.hasOwner() && !username(config.getOwner()))
			return INVALID_USERNAME;
		if (config.hasPort() && !port(config.getPort()))
			return INVALID_PORT;
		if (config.hasAddress() && !ipv4(config.getAddress()))
			return INVALID_ADDRESS;
		if (!config.hasCert() && config.hasKey())
			return INVALID_CERTIFICATE;
		if (config.hasCert() && !config.hasKey())
			return INVALID_KEY;
		if (config.hasCert() && config.hasKey()) {
			// Check certificate and key formats
			try {
				CertUtil.parseCert(config.getCert().toByteArray());
			} catch (CertificateException e) {
				return INVALID_CERTIFICATE;
			}

			try {
				CertUtil.parseKey(config.getKey().toByteArray());
			} catch (InvalidKeySpecException e) {
				return INVALID_KEY;
			}
		}

		return OK;
	}

	/**
	 * Check a {@link ProtoListener} for completeness. A {@link ProtoType} is
	 * complete if all required fields are present.
	 *
	 * @param config The candidate configuration
	 * @return An error code or {@link ErrorCode#OK}
	 */
	public static ErrorCode complete(ProtoListener config) {
		if (config == null)
			throw new IllegalArgumentException();

		if (!config.hasPort())
			return INVALID_PORT;
		if (!config.hasAddress())
			return INVALID_ADDRESS;
		if (!config.hasOwner())
			return INVALID_USERNAME;

		return OK;
	}

}
