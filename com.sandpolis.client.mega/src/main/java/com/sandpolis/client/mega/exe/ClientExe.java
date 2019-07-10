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
package com.sandpolis.client.mega.exe;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCClient.RQ_ClientMetadata;
import com.sandpolis.core.proto.net.MCClient.RS_ClientMetadata;
import com.sandpolis.core.proto.net.MSG;

/**
 * @author cilki
 * @since 5.0.2
 */
public class ClientExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ClientExe.class);

	@Auth
	public Message.Builder rq_client_metadata(RQ_ClientMetadata rq) throws Exception {
		return RS_ClientMetadata.newBuilder().setUsername(System.getProperty("user.name"))
				.setHostname(InetAddress.getLocalHost().getHostName());
	}

	@Auth
	public void rq_power_change(MSG.Message m) {
		var rq = m.getRqPowerChange();
		// TODO check permissions
		// TODO avoid switches
		switch (rq.getChange()) {
		case POWEROFF:
			// TODO
			break;
		case RESTART:
			// TODO
			break;
		default:
			break;
		}
	}

	public ClientExe(Sock connector) {
		super(connector);
	}

}