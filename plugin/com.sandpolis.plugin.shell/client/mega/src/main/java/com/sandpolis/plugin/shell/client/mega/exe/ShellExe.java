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

import java.io.InputStreamReader;

import com.google.common.io.CharStreams;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.instance.PlatformUtil;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.util.ProtoUtil;
import com.sandpolis.plugin.shell.client.mega.Shells;
import com.sandpolis.plugin.shell.net.MCShell.RQ_Execute;
import com.sandpolis.plugin.shell.net.MCShell.RQ_ListShells;
import com.sandpolis.plugin.shell.net.MCShell.RQ_PowerChange;
import com.sandpolis.plugin.shell.net.MCShell.RS_Execute;
import com.sandpolis.plugin.shell.net.MCShell.RS_ListShells;
import com.sandpolis.plugin.shell.net.MCShell.RS_ListShells.ShellListing;
import com.sandpolis.plugin.shell.net.MCShell.Shell;
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
		switch (rq.getType()) {
		case BASH:
			command = Shells.BASH.buildCommand(rq.getCommand());
			break;
		case PWSH:
			command = Shells.PWSH.buildCommand(rq.getCommand());
			break;
		case CMD:
			command = Shells.CMD.buildCommand(rq.getCommand());
			break;
		default:
			throw new RuntimeException();
		}

		Process p = Runtime.getRuntime().exec(command);
		try (var in = new InputStreamReader(p.getInputStream())) {
			int exit = p.waitFor();
			return RS_Execute.newBuilder().setResult(CharStreams.toString(in)).setExitCode(exit);
		}
	}

	@Auth
	@Handler(tag = MSG.ShellMessage.RQ_LIST_SHELLS_FIELD_NUMBER)
	public Message.Builder rq_list_shells(RQ_ListShells rq) throws Exception {
		var rs = RS_ListShells.newBuilder();

		if (Shells.PWSH.getLocation() != null) {
			rs.addListing(ShellListing.newBuilder().setType(Shell.PWSH).setLocation(Shells.PWSH.getLocation()));
		}
		if (Shells.BASH.getLocation() != null) {
			rs.addListing(ShellListing.newBuilder().setType(Shell.BASH).setLocation(Shells.BASH.getLocation()));
		}
		if (Shells.CMD.getLocation() != null) {
			rs.addListing(ShellListing.newBuilder().setType(Shell.CMD).setLocation(Shells.CMD.getLocation()));
		}

		return rs;
	}

	@Auth
	@Handler(tag = MSG.ShellMessage.RQ_POWER_CHANGE_FIELD_NUMBER)
	public void rq_power_change(RQ_PowerChange rq) throws Exception {
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
