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
package com.sandpolis.plugin.shell.client.mega.exe;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.PlatformUtil;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.util.ProtoUtil;
import com.sandpolis.plugin.shell.client.mega.CommandEncoders.BashEncoder;
import com.sandpolis.plugin.shell.client.mega.CommandEncoders.PowerShellEncoder;
import com.sandpolis.plugin.shell.net.MCShell.RQ_Execute;
import com.sandpolis.plugin.shell.net.MCShell.RS_Execute;
import com.sandpolis.plugin.shell.net.MSG;

public class ShellExe extends Exelet {

	@Override
	public String getPluginPrefix() {
		return "com.sandpolis.plugin.shell";
	}

	@Override
	public void reply(com.sandpolis.core.proto.net.MSG.Message msg, MessageOrBuilder payload) {
		connector.send(ProtoUtil.rs(msg, ProtoUtil.setPluginPayload(MSG.ShellMessage.newBuilder(), payload)));
	}

	@Override
	public Message extractPayload(com.sandpolis.core.proto.net.MSG.Message msg) {
		return ProtoUtil.getPayload(ProtoUtil.getPayload(msg));
	}

	@Auth
	@Handler(tag = MSG.ShellMessage.RQ_EXECUTE_FIELD_NUMBER)
	public Message.Builder rq_execute(RQ_Execute rq) throws Exception {

		String[] command;
		switch (PlatformUtil.queryOsType()) {
		case LINUX:
		case MACOS:
			command = BashEncoder.encode(rq.getCommand());
			break;
		case WINDOWS:
			command = PowerShellEncoder.encode(rq.getCommand());
			break;
		default:
			throw new RuntimeException();
		}

		Process p = Runtime.getRuntime().exec(command);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			int exit = p.waitFor();

			String line;
			StringBuffer buffer = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append("\n");
			}
			return RS_Execute.newBuilder().setResult(buffer.toString()).setExitCode(exit);
		}
	}
}
