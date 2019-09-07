/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.net.command;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.store.network.NetworkStore.NetworkStore;

import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant;
import com.sandpolis.core.instance.PoolConstant;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.util.ProtoUtil;

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
	private EventExecutor pool = ThreadStore.get(PoolConstant.net.message.incoming);

	/**
	 * The response timeout in milliseconds.
	 */
	private long timeout = Config.getInteger(ConfigConstant.net.message.default_timeout);

	/**
	 * The target CVID. Defaults to the default server CVID.
	 */
	private Integer cvid = NetworkStore.getPreferredServer();

	/**
	 * The target sock which will be used to send and receive messages. Defaults to
	 * the default server.
	 */
	protected Sock sock = ConnectionStore.get(cvid).get();

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
		this.timeout = Config.getInteger(checkNotNull(timeout));
		return (E) this;
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
	 * Explicitly set the remote endpoint by {@link Sock}.
	 *
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(Sock sock) {
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
		this.sock = ConnectionStore.get(cvid).get();
		return (E) this;
	}

	/**
	 * Explicitly set the remote endpoint.
	 *
	 * @param cvid The target CVID
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(int cvid, Sock sock) {
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

		return new ResponseFuture<>(pool, sock.request(ProtoUtil.setPayload(ProtoUtil.rq().setTo(cvid), payload),
				timeout, TimeUnit.MILLISECONDS));
	}

}
