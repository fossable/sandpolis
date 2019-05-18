/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.server.vanilla.exe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCPing.RS_Ping;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.server.vanilla.store.server.ServerStore;

/**
 * Message handlers for server requests.
 * 
 * @author cilki
 * @since 4.0.0
 */
public class ServerExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ServerExe.class);

	public ServerExe(Sock connector) {
		super(connector);
	}

	@Unauth
	public void rq_server_banner(Message m) {
		reply(m, ServerStore.getBanner());
	}

	@Unauth
	public void rq_ping(Message m) {
		reply(m, RS_Ping.newBuilder());
	}
}
