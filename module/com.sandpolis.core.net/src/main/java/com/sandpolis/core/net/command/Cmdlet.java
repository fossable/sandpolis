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
package com.sandpolis.core.net.command;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;

import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.net.util.MsgUtil;

import io.netty.util.concurrent.EventExecutor;

/**
 * A {@link Cmdlet} contains commands that can be run against a CVID.
 * {@link Cmdlet}s usually produce requests and the corresponding {@link Exelet}
 * returns a response.
 *
 * @author cilki
 * @since 5.0.0
 */
@SuppressWarnings("unchecked")
public abstract class Cmdlet<E extends Cmdlet<E>> {

	/**
	 * The {@link EventExecutor} that will be used to execute completion listeners.
	 */
	private EventExecutor pool = ThreadStore.get("net.message.incoming");

	/**
	 * The response timeout in milliseconds.
	 */
	private long timeout = Config.MESSAGE_TIMEOUT.value().get();

	/**
	 * The target CVID. Defaults to the default server CVID.
	 */
	private Integer cvid = NetworkStore.getPreferredServer();

	/**
	 * The target sock which will be used to send and receive messages. Defaults to
	 * the default server.
	 */
	protected Connection sock = ConnectionStore.getByCvid(cvid).orElse(null);

	/**
	 * Explicitly set a thread pool for the completion listeners.
	 *
	 * @param pool The pool class
	 * @return {@code this}
	 */
	public E pool(String pool) {
		this.pool = ThreadStore.get(checkNotNull(pool));
		return (E) this;
	}

	/**
	 * Set a session timeout for all {@link CommandSession}s spawned from this
	 * {@link Cmdlet}.
	 *
	 * @param timeout The timeout class
	 * @return {@code this}
	 */
	public E timeout(String timeout) {
		throw new UnsupportedOperationException();
		// return (E) this;
	}

	/**
	 * Set a session timeout for all {@link CommandSession}s spawned from this
	 * {@link Cmdlet}.
	 *
	 * @param timeout The timeout
	 * @param unit    The time unit
	 * @return {@code this}
	 */
	public E timeout(long timeout, TimeUnit unit) {
		this.timeout = unit.convert(timeout, TimeUnit.MILLISECONDS);
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint by {@link Connection}.
	 *
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(Connection sock) {
		this.sock = checkNotNull(sock);
		this.cvid = sock.getRemoteCvid();
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint by CVID.
	 *
	 * @param cvid The target CVID
	 * @return {@code this}
	 */
	public E target(int cvid) {
		this.cvid = cvid;
		this.sock = ConnectionStore.getByCvid(cvid).get();
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint.
	 *
	 * @param cvid The target CVID
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(int cvid, Connection sock) {
		this.cvid = cvid;
		this.sock = checkNotNull(sock);
		return (E) this;
	}

	/**
	 * Start a new command session.
	 *
	 * @return A new {@link CommandSession}
	 */
	protected CommandSession begin() {
		return new CommandSession(pool, cvid, sock, timeout);
	}

	/**
	 * Send the given request according to the current configuration.
	 *
	 * @param payload The message payload
	 * @return A new response future
	 */
	protected <R extends Message> ResponseFuture<R> request(MessageOrBuilder payload) {
		checkNotNull(payload);

		return new ResponseFuture<>(pool,
				sock.request(MsgUtil.rq(payload).setTo(cvid), timeout, TimeUnit.MILLISECONDS));
	}

}
