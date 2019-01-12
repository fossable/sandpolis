/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.core.net;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.util.ProtoUtil;

/**
 * A {@link Cmdlet} is the opposite of an {@link Exelet} in a sense. Cmdlets
 * usually produce a message and exelets handle them.<br>
 * <br>
 * 
 * @author cilki
 * @since 5.0.0
 */
@SuppressWarnings("unchecked")
public abstract class Cmdlet<E> {

	/**
	 * The target CVID which will be used in {@link #route}. If {@code null}, the
	 * default server will be used.
	 */
	private Integer cvid;

	/**
	 * The target sock which will be used in {@link #route}. If {@code null}, the
	 * default server will be used.
	 */
	private Sock sock;

	/**
	 * The ID of the {@link ExecutorService} that will be used to execute response
	 * listeners. If {@code null}, the default will be used.
	 */
	private String poolId;

	/**
	 * The response timeout class.
	 */
	private String timeout = "net.timeout.response.default";

	/**
	 * Explicitly set a thread pool for the response listeners.
	 * 
	 * @param type The pool type
	 * @return {@code this}
	 */
	public E pool(String type) {
		this.poolId = Objects.requireNonNull(type);
		return (E) this;
	}

	/**
	 * Explicitly set a response timeout.
	 * 
	 * @param timeout The timeout class
	 * @return {@code this}
	 */
	public E timeout(String timeout) {
		this.timeout = Objects.requireNonNull(timeout);
		return (E) this;
	}

	/**
	 * Explicitly set the recipient {@link Sock}.
	 * 
	 * @param sock The target sock
	 * @return {@code this}
	 */
	public E target(Sock sock) {
		this.sock = Objects.requireNonNull(sock);
		return (E) this;
	}

	/**
	 * Explicitly set the recipient CVID.
	 * 
	 * @param cvid The target CVID
	 * @return {@code this}
	 */
	public E target(int cvid) {
		this.cvid = cvid;
		return (E) this;
	}

	/**
	 * Route the given payload according to the current configuration.
	 * 
	 * @param payload The message payload
	 * @return A new response future
	 */
	protected <R> ResponseFuture<R> rq(MessageOrBuilder payload) {
		Objects.requireNonNull(payload);
		return route(ProtoUtil.setPayload(ProtoUtil.rq(), payload));
	}

	/**
	 * Route the given message according to the current configuration.
	 * 
	 * @param m The message to send
	 * @return A new response future
	 */
	protected <R> ResponseFuture<R> route(Message.Builder m) {
		Objects.requireNonNull(m);

		MessageFuture mf;
		if (sock != null) {
			m.setTo(sock.getRemoteCvid());
			mf = sock.request(m);
		} else if (cvid != null) {
			m.setTo(cvid);
			mf = NetworkStore.route(m, timeout);
		} else {
			mf = NetworkStore.route(m, timeout);
		}

		if (poolId != null) {
			return new ResponseFuture<>(ThreadStore.get(poolId), mf);
		} else {
			return new ResponseFuture<>(mf);
		}
	}
}
