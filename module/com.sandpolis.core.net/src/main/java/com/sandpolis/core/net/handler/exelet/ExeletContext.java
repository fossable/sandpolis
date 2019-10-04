package com.sandpolis.core.net.handler.exelet;

import java.util.Objects;

import com.google.protobuf.Message;
import com.sandpolis.core.net.sock.Sock;

/**
 * An object that can optionally be passed to {@link Exelet} handlers and
 * provides connection-specific utilities.
 * 
 * @author cilki
 * @since 5.1.0
 */
public final class ExeletContext {

	public final Sock connector;

	Message.Builder reply;

	Runnable deferAction;

	public ExeletContext(Sock connector) {
		this.connector = Objects.requireNonNull(connector);
	}

	/**
	 * Set the response payload.
	 * 
	 * @param msg The response payload
	 */
	public void reply(Message.Builder msg) {
		if (this.reply != null)
			throw new IllegalStateException();

		this.reply = msg;
	}

	/**
	 * Schedule an action to be executed immediately after the response is sent. Do
	 * not call {@link #reply(Message.Builder)} from this block.
	 * 
	 * @param action The deferred action
	 */
	public void defer(Runnable action) {
		if (this.deferAction != null)
			throw new IllegalStateException();

		this.deferAction = action;
	}
}
