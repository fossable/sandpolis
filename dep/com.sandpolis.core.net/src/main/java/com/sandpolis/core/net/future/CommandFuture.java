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
package com.sandpolis.core.net.future;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ProtoUtil;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

/**
 * A {@link Future} that represents the completion of a {@link Cmdlet} command.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class CommandFuture extends DefaultPromise<Outcome> {

	public static interface ResponseHandler<E> {
		public void handle(E e) throws Exception;
	}

	/**
	 * The in-progress outcome of the command.
	 */
	private Outcome.Builder outcome;

	/**
	 * The executing or waiting components of the command.
	 */
	private Set<Future<?>> components;

	public CommandFuture() {
		this(ThreadStore.get("net.message.incoming"));
	}

	public CommandFuture(EventExecutor executor) {
		super(executor);

		outcome = ProtoUtil.begin();
		components = new HashSet<>();
	}

	public <R> void add(Future<R> future, ResponseHandler<R> handler, ResponseHandler<Outcome> failure) {
		checkNotNull(future);
		checkNotNull(handler);
		checkArgument(future != this);

		synchronized (components) {
			components.add(future);
		}

		future.addListener(f -> {
			if (f.isSuccess()) {
				try {
					handler.handle((R) f.get());
				} catch (ClassCastException e) {
					if (failure != null && f.get() instanceof Outcome) {
						try {
							failure.handle((Outcome) f.get());
						} catch (Exception t) {
							setFailure(t);
							return;
						}
					} else {
						// No failure handler specified or unknown response type
						setFailure(new Exception("Failed to handle message: " + f.get().getClass().getName()));
						return;
					}
				} catch (Exception e) {
					setFailure(e);
					return;
				}

				remove(future);
				tryComplete();
			} else {
				setFailure(f.cause());
			}
		});
	}

	public CommandFuture success() {
		// TODO
		return this;
	}

	public CommandFuture failed() {
		// TODO
		return this;
	}

	/**
	 * Terminate the entire command session.
	 * 
	 * @param message The failure message
	 * @return {@code this}
	 */
	public CommandFuture abort(String message) {
		abort();
		setFailure(new Exception(message));
		return this;
	}

	/**
	 * Terminate the entire command session.
	 * 
	 * @param exception The cause
	 * @return {@code this}
	 */
	public CommandFuture abort(Throwable exception) {
		abort();
		setFailure(exception);
		return this;
	}

	/**
	 * Terminate the entire command session.
	 * 
	 * @return {@code this}
	 */
	private void abort() {
		synchronized (components) {
			components.removeIf(future -> {
				future.cancel(true);
				return true;
			});
		}
	}

	/**
	 * Complete the command session if all components have been completed.
	 */
	private void tryComplete() {
		synchronized (components) {
			if (components.size() == 0)
				setSuccess(ProtoUtil.success(outcome));
		}
	}

	private void remove(Object o) {
		synchronized (components) {
			components.remove(o);
		}
	}
}
