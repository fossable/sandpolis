/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.core.net.command;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.PoolConstant.net;
import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ProtoUtil;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

/**
 * A {@link CommandSession} contains the state of a command.
 * 
 * @author cilki
 * @since 5.0.2
 */
public class CommandSession extends DefaultPromise<Outcome> implements CommandFuture {

	private static final Logger log = LoggerFactory.getLogger(CommandSession.class);

	/**
	 * The remote endpoint's CVID.
	 */
	private Integer cvid;

	/**
	 * A {@link Sock} that leads to the remote endpoint. If there's only one hop,
	 * the CVID of {@link #sock} will be equal to {@link #cvid}.
	 */
	private Sock sock;

	/**
	 * An in-progress outcome for the command.
	 */
	private Outcome.Builder outcome;

	/**
	 * Actions that make up the overall command.
	 */
	private List<Future<?>> components;

	public CommandSession(CommandSession parent) {
		this();
		parent.add(this);

		this.cvid = parent.cvid;
		this.sock = parent.sock;
	}

	public CommandSession() {
		this((EventExecutor) ThreadStore.get(net.message.incoming));
	}

	public CommandSession(EventExecutor executor) {
		super(executor);

		outcome = ProtoUtil.begin();
		components = new LinkedList<>();
	}

	public CommandSession(Integer cvid, Sock sock) {
		this();
		checkArgument(cvid != null || sock != null);

		if (cvid == null)
			cvid = sock.getRemoteCvid();
		if (sock == null)
			// TODO get sock from NetworkStore
			throw new RuntimeException();

		this.cvid = Objects.requireNonNull(cvid);
		this.sock = Objects.requireNonNull(sock);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <R extends Message> void add(Future<R> future, MessageHandler... handlers) {
		checkNotNull(future);
		checkArgument(future != this);

		components.add(future);

		future.addListener(f -> {

			if (future.isSuccess()) {
				for (var handler : handlers) {
					try {
						handler.handle(future.get());
						break;
					} catch (ClassCastException e) {
						// Try the next handler
						continue;
					} catch (Exception e) {
						abort(e);
						return;
					}
				}

				tryComplete();
			} else {
				abort(future.cause());
			}
		});
	}

	/**
	 * Immediately complete the command with a postive outcome.
	 * 
	 * @return {@code this}
	 */
	public CommandSession success() {
		complete(outcome.setTime(System.currentTimeMillis() - outcome.getTime()).setResult(true).build());
		return this;
	}

	/**
	 * Complete the command with the given outcome. The command will enter the
	 * success state regardless of the outcome contents.
	 * 
	 * @param outcome The command outcome
	 * @return {@code this}
	 */
	public CommandSession complete(Outcome outcome) {
		setSuccess(outcome);
		return this;
	}

	/**
	 * Halt the entire command. All components will be interrupted and the command
	 * will enter the failed state.
	 * 
	 * @param message The failure message
	 */
	public void abort(String message) {
		components.forEach(future -> future.cancel(true));
		setFailure(new Exception(message));
	}

	/**
	 * Halt the entire command. All components will be interrupted and the command
	 * will enter the failed state.
	 * 
	 * @param cause The exception that aborted the command
	 */
	public void abort(Throwable cause) {
		components.forEach(future -> future.cancel(true));
		setFailure(cause);
	}

	public CommandSession failure(ErrorCode code) {
		complete(outcome.setTime(System.currentTimeMillis() - outcome.getTime()).setResult(false).setError(code)
				.build());
		return this;
	}

	/**
	 * Complete the overall command session if all components have been completed.
	 */
	private void tryComplete() {
		checkState(!isDone());

		if (components.stream().allMatch(future -> future.isDone()))
			setSuccess(ProtoUtil.success(outcome));
	}

	/**
	 * 
	 * 
	 * @param future
	 */
	public void subcommand(CommandFuture future) {
		checkArgument(future instanceof CommandSession);

		add(future);
	}

	/**
	 * 
	 * 
	 * @param future
	 * @param handler
	 */
	public void subcommand(CommandFuture future, MessageHandler<Outcome> handler) {
		checkArgument(future instanceof CommandSession);
		checkNotNull(handler);

		add(future, handler);
	}

	/**
	 * Send a message to the remote endpoint without expecting a response.
	 * 
	 * @param payload The message payload
	 */
	public void send(MessageOrBuilder payload) {
		checkNotNull(payload);

		sock.send(ProtoUtil.setPayload(MSG.Message.newBuilder().setTo(cvid), payload));
	}

	/**
	 * Send a request to the remote endpoint and register the given handlers for the
	 * response.
	 * 
	 * @param payload  The message payload
	 * @param handlers The response handlers
	 */
	public void request(MessageOrBuilder payload, MessageHandler<?>... handlers) {
		var rq = ProtoUtil.setPayload(ProtoUtil.rq().setTo(cvid), payload);

		var future = new ResponseFuture<>(ThreadStore.get(""), sock.request(rq));
		add(future, handlers);
	}

}
