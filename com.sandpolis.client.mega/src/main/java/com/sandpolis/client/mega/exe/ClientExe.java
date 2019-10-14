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
package com.sandpolis.client.mega.exe;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.util.PlatformUtil;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgClient.RQ_ClientMetadata;
import com.sandpolis.core.proto.net.MsgClient.RS_ClientMetadata;

public final class ClientExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ClientExe.class);

	@Auth
	@Handler(tag = MSG.RQ_CLIENT_METADATA_FIELD_NUMBER)
	public static MessageOrBuilder rq_client_metadata(RQ_ClientMetadata rq) throws Exception {
		log.trace("rq_client_metadata");

		return RS_ClientMetadata.newBuilder()
				// Network hostname
				.setHostname(InetAddress.getLocalHost().getHostName())
				// OS Family
				.setOs(PlatformUtil.OS_TYPE)
				// Base directory location
				.setInstallDirectory(Environment.BASE.toString());
	}

	private ClientExe() {
	}
}
