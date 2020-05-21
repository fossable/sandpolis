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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Result.Outcome;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.net.util.ProtoUtil;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

/**
 * A {@link CommandSession} maintains the state of an interaction between the
 * local instance and a remote endpoint.<br>
 * <br>
 * A {@code CommandSession} is considered successful if all of its components
 * are successful. If any component fails with an exception, the entire session
 * fails with the same exception. Sessions can also be explicitly aborted or
 * succeeded from a handler context using {@link #abort(Throwable)} and
 * {@link #success()}.
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
	 * A {@link Connection} that leads to the remote endpoint. If there's only one
	 * hop, the CVID of {@link #gateway} will be equal to {@link #cvid}.
	 */
	private Connection gateway;

	/**
	 * The in-progress outcome of the command.
	 */
	private Outcome.Builder outcome;

	/**
	 * Actions that compose the overall command.
	 */
	private List<Future<?>> components;

	/**
	 * The command timeout in milliseconds.
	 */
	private long timeout;

	public CommandSession(EventExecutor executor, Integer cvid, Connection sock, long timeout) {
		super(executor);
		checkArgument(cvid != null || sock != null);

		// TODO replace
		// outcome = ProtoUtil.begin();
		outcome = Outcome.newBuilder().setTime(System.currentTimeMillis());
		components = new LinkedList<>();

		if (cvid == null)
			cvid = sock.getRemoteCvid();
		else if (sock == null)
			// TODO get sock from NetworkStore
			sock = ConnectionStore.get(cvid).get();

		this.cvid = checkNotNull(cvid);
		this.gateway = checkNotNull(sock);
		this.timeout = timeout;
	}

	/**
	 * Add a new component to the {@link CommandSession}.
	 *
	 * @param <R>      The component's expected result type
	 * @param future   The component
	 * @param handlers A list of completion handlers
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <R extends Message> void addComponent(Future<R> future, MessageHandler... handlers) {
		checkNotNull(future);
		checkArgument(future != this);

		components.add(future);

		future.addListener(f -> {

			if (future.isSuccess()) {
				boolean handled = false;
				R rs = future.get();

				for (var handler : handlers) {
					try {
						handler.handle(rs);
						handled = true;
						break;
					} catch (ClassCastException e) {
						if (CommandSession.class.getName().equals(e.getStackTrace()[0].getClassName()))
							// The exception came from the act of passing the message to the handler and not
							// from the handler itself. Continue to the next handler.
							continue;

						abort(e);
						return;
					} catch (Exception e) {
						abort(e);
						return;
					}
				}

				if (!handled && handlers.length > 0) {
					abort("Failed to handle message");
					return;
				}

				tryComplete();
			} else {
				if (!isDone())
					abort(future.cause());
			}
		});
	}

	/**
	 * Complete the session if all components have been completed.
	 */
	private void tryComplete() {
		if (!isDone() && components.stream().allMatch(future -> future.isDone()))
			// TODO replace
			// setSuccess(ProtoUtil.success(outcome));
			setSuccess(outcome.setResult(true).setTime(System.currentTimeMillis() - outcome.getTime()).build());
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
	 * Immediately complete the command with a postive outcome.
	 *
	 * @return {@code this}
	 */
	public CommandSession success() {
		complete(outcome.setTime(System.currentTimeMillis() - outcome.getTime()).setResult(true).build());
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
		log.error("Command aborted: {}", message);
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
		log.error("Command aborted", cause);
	}

	/**
	 * Add a subcommand to the current session.
	 *
	 * @param future The subsession
	 * @return {@code future}
	 */
	public CommandFuture sub(CommandFuture future) {
		checkArgument(future instanceof CommandSession);

		addComponent(future);
		return future;
	}

	/**
	 * Add a subcommand to the current session.
	 *
	 * @param future  The subsession
	 * @param handler The subsession's completion handler
	 * @return {@code future}
	 */
	public CommandFuture sub(CommandFuture future, MessageHandler<Outcome> handler) {
		checkArgument(future instanceof CommandSession);
		checkNotNull(handler);

		addComponent(future, handler);
		return future;
	}

	/**
	 * Send a message to the remote endpoint without expecting a response.
	 *
	 * @param payload The message payload
	 */
	public void send(MessageOrBuilder payload) {
		checkNotNull(payload);

		gateway.send(ProtoUtil.setPayload(MSG.newBuilder().setTo(cvid), payload));
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

		var future = new ResponseFuture<>(ThreadStore.get("net.message.incoming"),
				gateway.request(rq, timeout, TimeUnit.MILLISECONDS));
		addComponent(future, handlers);
	}

	/**
	 * Build a {@code String} representation of this {@link CommandSession} for
	 * debugging purposes.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(format(this));

		for (Future<?> f : components) {
			if (f instanceof CommandSession) {
				String subcommand = f.toString();

				// Insert indention
				subcommand = "\t" + subcommand.replaceAll("\n", "\n\t");
				sb.append(subcommand);
			} else {
				sb.append("\t");
				sb.append(format(f));
			}
		}
		return sb.toString();
	}

	private String format(Future<?> f) {
		return String.format("%s: %4s %7s %9s%n", f.getClass().getSimpleName(), f.isDone() ? "done" : "wait",
				f.isDone() ? (f.isSuccess() ? "success" : "failure") : "", f.isCancelled() ? "cancelled" : "");
	}

}
