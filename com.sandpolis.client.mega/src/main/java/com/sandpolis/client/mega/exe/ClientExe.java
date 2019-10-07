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
import java.nio.file.Paths;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.util.PlatformUtil;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCClient.RQ_ClientMetadata;
import com.sandpolis.core.proto.net.MCClient.RS_ClientMetadata;
import com.sandpolis.core.proto.net.MSG;

/**
 * @author cilki
 * @since 5.0.2
 */
public final class ClientExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ClientExe.class);

	@Auth
	@Handler(tag = MSG.Message.RQ_CLIENT_METADATA_FIELD_NUMBER)
	public static MessageOrBuilder rq_client_metadata(RQ_ClientMetadata rq) throws Exception {

		return RS_ClientMetadata.newBuilder().setUsername(System.getProperty("user.name"))
				.setOsVersion(System.getProperty("os.name") + " " + System.getProperty("os.version"))
				.setHostname(InetAddress.getLocalHost().getHostName()).setOsType(PlatformUtil.OS_TYPE)
				.setTimezone(TimeZone.getDefault().getID()).setStartTimestamp(Environment.JVM_TIMESTAMP.getTime())
				.setUserhome(Paths.get(System.getProperty("user.home")).toUri().getPath());

	}

	private ClientExe() {
	}
}
