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

import java.util.LinkedList;
import java.util.List;

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

	private Outcome.Builder outcome;

	private List<ResponseFuture<?>> responses;

	public CommandFuture() {
		this(ThreadStore.get("net.message.incoming"));
	}

	public CommandFuture(EventExecutor executor) {
		super(executor);

		outcome = ProtoUtil.begin();
		responses = new LinkedList<>();
	}

	public void add(ResponseFuture<?> future) {
		responses.add(future);
	}

	public void tryComplete(ResponseFuture<?> rf) {

		// If the given future is the last, the command is complete
		if (responses.indexOf(rf) == responses.size() - 1)
			setSuccess(ProtoUtil.success(outcome));
	}
}
