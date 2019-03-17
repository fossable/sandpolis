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
package com.sandpolis.viewer.cmd;

import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MCServer.RQ_ServerBanner;
import com.sandpolis.core.proto.net.MCServer.RS_ServerBanner;

/**
 * Contains server commands.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class ServerCmd extends Cmdlet<ServerCmd> {

	public ResponseFuture<RS_ServerBanner> getServerBanner() {
		return route(RQ_ServerBanner.newBuilder());
	}

	/**
	 * Prepare for an asynchronous command.
	 * 
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link ServerCmd} can be invoked
	 */
	public static ServerCmd async() {
		return new ServerCmd();
	}

	private ServerCmd() {
	}
}
