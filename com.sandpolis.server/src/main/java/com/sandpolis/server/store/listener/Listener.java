/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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
package com.sandpolis.server.store.listener;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.util.Listener.ListenerConfig;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.net.init.ServerInitializer;
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

import io.netty.bootstrap.ServerBootstrap;
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
@Table(name = "Listeners")
public class Listener {

	public static final Logger log = LoggerFactory.getLogger(Listener.class);

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The listener's unique ID.
	 */
	@Column(nullable = false, unique = true)
	private long id;

	/**
	 * The listener's optional user-friendly name.
	 */
	@Column(nullable = true)
	private String name;

	/**
	 * The user that owns the listener.
	 */
	@ManyToOne(optional = true, cascade = CascadeType.ALL)
	@JoinColumn(referencedColumnName = "username")
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
	@Column(nullable = true)
	private boolean upnp;

	/**
	 * Indicates whether client instances can be accepted by the listener.
	 */
	@Column(nullable = true)
	private boolean clientAcceptor;

	/**
	 * Indicates whether viewer instances can be accepted by the listener.
	 */
	@Column(nullable = true)
	private boolean viewerAcceptor;

	/**
	 * The listener's certificate.
	 */
	@Column(nullable = true)
	private byte[] certificate;

	/**
	 * The listener's private key.
	 */
	@Column(nullable = true)
	private byte[] privateKey;

	/**
	 * The listening {@code Channel} that is bound to the listening network
	 * interface.
	 */
	@Transient
	private ServerChannel acceptor;

	/**
	 * The {@code EventLoopGroup} that handles the {@code ServerChannel}.
	 */
	@Transient
	private EventLoopGroup parentLoopGroup;

	/**
	 * The {@code EventLoopGroup} that handles all spawned {@code Channel}s.
	 */
	@Transient
	private EventLoopGroup childLoopGroup;

	/**
	 * Construct a new {@code Listener} according to the given configuration.
	 * 
	 * @param config The {@code Listener}'s configuration
	 */
	public Listener(ListenerConfig config) {
		if (!ValidationUtil.listenerConfig(config))
			throw new IllegalArgumentException("Invalid listener configuration");

		id = config.getId();
		name = config.getName();
		owner = UserStore.get(config.getOwner());
		enabled = config.getEnabled();
		port = config.getPort();
		address = config.getAddress();
		upnp = config.getUpnp();
		clientAcceptor = config.getClientAcceptor();
		viewerAcceptor = config.getViewerAcceptor();

		// Optional certificate
		if (config.getCert() != null && config.getKey() != null) {
			certificate = config.getCert().toByteArray();
			privateKey = config.getKey().toByteArray();
		}
	}

	/**
	 * Start the listener.
	 */
	public boolean start() {
		if (isListening())
			throw new IllegalStateException();

		log.debug("Starting listener on port: {}", port);

		parentLoopGroup = Sock.TRANSPORT.getEventLoopGroup();
		childLoopGroup = Sock.TRANSPORT.getEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap();
		b.group(parentLoopGroup, childLoopGroup).channel(Sock.TRANSPORT.getServerSocketChannel())//
				.childHandler(new ServerInitializer())// (certificate, privateKey)
				.option(ChannelOption.SO_BACKLOG, 128)//
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		try {
			acceptor = (ServerChannel) b.bind(address, port).await().channel();
		} catch (InterruptedException e) {
			log.error("Failed to start the listener because the thread was interrupted.");
			acceptor = null;
			return false;
		}
		return true;
	}

	/**
	 * Stop the listener, leaving all spawned {@code Channel}s alive.
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

	/**
	 * Construct a {@link ListenerConfig} that represents this listener.
	 * 
	 * @return A new {@link ListenerConfig}
	 */
	public ListenerConfig getConfig() {

		ListenerConfig.Builder config = ListenerConfig.newBuilder();
		config.setId(id).setName(name).setEnabled(enabled).setPort(port).setAddress(address).setUpnp(upnp)
				.setClientAcceptor(clientAcceptor).setViewerAcceptor(viewerAcceptor);

		if (certificate != null && privateKey != null)
			config.setCert(ByteString.copyFrom(certificate)).setKey(ByteString.copyFrom(privateKey));

		return config.build();
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

}
