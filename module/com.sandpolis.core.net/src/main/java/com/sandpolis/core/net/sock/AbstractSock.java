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
package com.sandpolis.core.net.sock;

import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.store.plugin.Events.PluginLoadedEvent;
import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.handler.exelet.ExeletHandler;
import com.sandpolis.core.net.init.AbstractChannelInitializer;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.core.net.store.connection.ConnectionStoreEvents.SockEstablishedEvent;
import com.sandpolis.core.net.store.connection.ConnectionStoreEvents.SockLostEvent;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * This class manages a network connection between the localhost and some remote
 * host. Underneath, this class uses a {@link Channel} instance regardless of
 * the protocol (TCP or UDP).
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class AbstractSock implements Sock {

	private static final Logger log = LoggerFactory.getLogger(AbstractSock.class);

	private final Promise<Void> handshakeFuture;

	/**
	 * The {@link Sock}'s underlying {@link Channel}.
	 */
	private final Channel channel;

	@Override
	public Channel channel() {
		return channel;
	}

	/**
	 * Build a new {@link Sock} around the given {@link Channel}.
	 *
	 * @param channel An active or inactive {@link Channel}
	 */
	public AbstractSock(Channel channel) {
		this.channel = Objects.requireNonNull(channel);
		this.handshakeFuture = channel.eventLoop().newPromise();

		channel.attr(ChannelConstant.SOCK).set(this);
		channel.attr(ChannelConstant.AUTH_STATE).set(false);
		channel.attr(ChannelConstant.CERTIFICATE_STATE).set(false);
	}

	@Subscribe
	private void onPluginLoaded(PluginLoadedEvent event) {
		ExeletHandler handler = getHandler(AbstractChannelInitializer.EXELET);
		if (handler != null) {
			event.get().getExtensions(ExeletProvider.class).forEach(provider -> {
				handler.register(event.get().getId(), provider.getMessageType(), provider.getExelets());
			});
		}
	}

	public void onActivityChanged(boolean activity) {
		if (!activity) {
			PluginStore.unregister(this);
			ConnectionStore.removeValue(this);
			ConnectionStore.postAsync(SockLostEvent::new, this);
		}
	}

	public void onCvidCompleted(boolean success) {
		if (success) {
			PluginStore.register(this);
			ConnectionStore.add(this);
			ConnectionStore.postAsync(SockEstablishedEvent::new, this);
			handshakeFuture.setSuccess(null);
		} else {
			handshakeFuture.setFailure(new Exception());
		}
	}

	@Override
	public Future<Void> getHandshakeFuture() {
		return handshakeFuture;
	}

	@Override
	public int hashCode() {
		return channel.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AbstractSock)
			return channel.equals(((AbstractSock) obj).channel);
		return false;
	}

}
