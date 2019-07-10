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
package com.sandpolis.core.stream.store;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.SubmissionPublisher;

import com.google.protobuf.MessageOrBuilder;

/**
 * @author cilki
 * @since 5.0.2
 */
public abstract class StreamSource<E extends MessageOrBuilder> extends SubmissionPublisher<E> {

	/**
	 * Immediately halt the flow of events from the source.
	 */
	public abstract void stop();

	/**
	 * Begin the flow of events from the source.
	 */
	public abstract void start();

	public void addOutbound(OutboundStreamAdapter<E> out) {
		checkArgument(!isSubscribed(out));
		subscribe(out);
		StreamStore.outbound.add(out);
	}

	public void addSink(StreamSink<E> s) {
		checkArgument(!isSubscribed(s));
		subscribe(s);
		StreamStore.sink.add(s);
	}
}