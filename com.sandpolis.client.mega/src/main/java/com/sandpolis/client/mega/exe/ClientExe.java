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

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.PlatformUtil;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCClient.RQ_ClientMetadata;
import com.sandpolis.core.proto.net.MCClient.RS_ClientMetadata;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.proto.util.Result.Outcome;

/**
 * @author cilki
 * @since 5.0.2
 */
public class ClientExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(ClientExe.class);

	@Auth
	@Handler(tag = MSG.Message.RQ_CLIENT_METADATA_FIELD_NUMBER)
	public Message.Builder rq_client_metadata(RQ_ClientMetadata rq) throws Exception {

		try {
			return RS_ClientMetadata.newBuilder().setUsername(System.getProperty("user.name"))
					.setOsVersion(System.getProperty("os.name") + " " + System.getProperty("os.version"))
					.setHostname(InetAddress.getLocalHost().getHostName()).setOsType(PlatformUtil.queryOsType())
					.setTimezone(TimeZone.getDefault().getID()).setStartTimestamp(Environment.JVM_TIMESTAMP.getTime())
					.setUserhome(Paths.get(System.getProperty("user.home")).toUri().getPath());
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_POWER_CHANGE_FIELD_NUMBER)
	public void rq_power_change(MSG.Message m) throws InterruptedException, IOException {
		var rq = m.getRqPowerChange();
		reply(m, Outcome.newBuilder().setResult(true));
		// TODO check permissions
		// TODO avoid switches
		switch (PlatformUtil.queryOsType()) {
		case LINUX:
			switch (rq.getChange()) {
			case POWEROFF:
				Runtime.getRuntime().exec("sudo poweroff").waitFor();
				break;
			case RESTART:
				Runtime.getRuntime().exec("sudo reboot").waitFor();
				break;
			default:
				break;
			}
			break;
		case MACOS:
			switch (rq.getChange()) {
			case POWEROFF:
				Runtime.getRuntime().exec("sudo shutdown -h now").waitFor();
				break;
			case RESTART:
				Runtime.getRuntime().exec("sudo shutdown -r now").waitFor();
				break;
			default:
				break;
			}
			break;
		case WINDOWS:
			switch (rq.getChange()) {
			case POWEROFF:
				Runtime.getRuntime().exec("shutdown /p").waitFor();
				break;
			case RESTART:
				Runtime.getRuntime().exec("shutdown /r").waitFor();
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}

		System.exit(0);
	}

}
