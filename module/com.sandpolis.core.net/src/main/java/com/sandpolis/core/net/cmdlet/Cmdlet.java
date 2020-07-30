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
package com.sandpolis.core.net.cmdlet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;

import java.util.concurrent.CompletionStage;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.exelet.Exelet;

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
	 * The target CVID. Defaults to the default server CVID.
	 */
	private Integer cvid = NetworkStore.getPreferredServer();

	/**
	 * The target sock which will be used to send and receive messages. Defaults to
	 * the default server.
	 */
	protected Connection target = ConnectionStore.getByCvid(cvid).orElse(null);

	/**
	 * Explicitly set the remote endpoint by {@link Connection}.
	 *
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(Connection sock) {
		this.target = checkNotNull(sock);
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
		this.target = ConnectionStore.getByCvid(cvid).get();
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
		this.target = checkNotNull(sock);
		return (E) this;
	}

	/**
	 * Alias for {@code target.request(responseType, payload)}.
	 * 
	 * @param <T>          The expected response type
	 * @param responseType The expected response type
	 * @param payload      The request payload
	 * @return An asynchronous {@link CompletionStage}
	 */
	protected <T extends Message> CompletionStage<T> request(Class<T> responseType, MessageOrBuilder payload) {
		return target.request(responseType, payload);
	}
}
