//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.cmdlet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.s7s.core.instance.connection.ConnectionStore.ConnectionStore;
import static org.s7s.core.instance.network.NetworkStore.NetworkStore;

import java.util.concurrent.CompletionStage;

import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.connection.Connection;
import org.s7s.core.instance.exelet.Exelet;

/**
 * A {@link Cmdlet} contains commands that can be run against a SID.
 * {@link Cmdlet}s usually produce requests and the corresponding {@link Exelet}
 * returns a response.
 *
 * @author cilki
 * @since 5.0.0
 */
@SuppressWarnings("unchecked")
public abstract class Cmdlet<E extends Cmdlet<E>> {

	/**
	 * The target SID. Defaults to the default server SID.
	 */
	private Integer sid = NetworkStore.getPreferredServer().orElse(0);

	/**
	 * The target sock which will be used to send and receive messages. Defaults to
	 * the default server.
	 */
	protected Connection target = ConnectionStore.getBySid(sid).orElse(null);

	/**
	 * Explicitly set the remote endpoint by {@link Connection}.
	 *
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(Connection sock) {
		this.target = checkNotNull(sock);
		this.sid = sock.get(ConnectionOid.REMOTE_SID).asInt();
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint by SID.
	 *
	 * @param sid The target SID
	 * @return {@code this}
	 */
	public E target(int sid) {
		this.sid = sid;
		this.target = ConnectionStore.getBySid(sid).get();
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint.
	 *
	 * @param sid  The target SID
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(int sid, Connection sock) {
		this.sid = sid;
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
	protected <T> CompletionStage<T> request(Class<T> responseType, MessageLiteOrBuilder payload) {
		return target.request(responseType, payload);
	}
}
