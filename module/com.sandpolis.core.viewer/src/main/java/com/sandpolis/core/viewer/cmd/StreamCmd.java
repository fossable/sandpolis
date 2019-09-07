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
package com.sandpolis.core.viewer.cmd;

import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MCStream.ProfileStreamParam;
import com.sandpolis.core.proto.net.MCStream.RQ_StreamStart;
import com.sandpolis.core.proto.net.MCStream.RQ_StreamStop;
import com.sandpolis.core.proto.net.MCStream.RS_StreamStart;
import com.sandpolis.core.proto.net.MCStream.StreamParam;
import com.sandpolis.core.proto.net.MCStream.StreamParam.Direction;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * Stream commands.
 *
 * @author cilki
 * @since 5.0.2
 */
public class StreamCmd extends Cmdlet<StreamCmd> {

	public ResponseFuture<RS_StreamStart> startProfileStream() {
		return request(RQ_StreamStart.newBuilder().setParam(
				StreamParam.newBuilder().setDirection(Direction.REVERSE).setProfile(ProfileStreamParam.newBuilder())));
	}

	/**
	 * Stop the given stream.
	 *
	 * @param streamID The ID of the stream to stop
	 * @return A response future
	 */
	public ResponseFuture<Outcome> stop(int streamID) {
		return request(RQ_StreamStop.newBuilder().setStreamID(streamID));
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link StreamCmd} can be invoked
	 */
	public static StreamCmd async() {
		return new StreamCmd();
	}

	private StreamCmd() {
	}
}
