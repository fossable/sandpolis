//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.plugin.shell.client.mega.exe;

import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.net.stream.StreamStore.StreamStore;

import java.io.InputStreamReader;

import com.google.common.io.CharStreams;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.net.stream.InboundStreamAdapter;
import com.sandpolis.core.net.stream.OutboundStreamAdapter;
import com.sandpolis.core.util.SystemUtil;
import com.sandpolis.plugin.shell.MessageShell.ShellMSG;
import com.sandpolis.plugin.shell.MsgPower.RQ_PowerChange;
import com.sandpolis.plugin.shell.MsgShell.EV_ShellStream;
import com.sandpolis.plugin.shell.MsgShell.RQ_Execute;
import com.sandpolis.plugin.shell.MsgShell.RQ_ListShells;
import com.sandpolis.plugin.shell.MsgShell.RQ_ShellStream;
import com.sandpolis.plugin.shell.MsgShell.RS_Execute;
import com.sandpolis.plugin.shell.MsgShell.RS_ListShells;
import com.sandpolis.plugin.shell.MsgShell.RS_ListShells.ShellListing;
import com.sandpolis.plugin.shell.MsgShell.Shell;
import com.sandpolis.plugin.shell.client.mega.Shells;
import com.sandpolis.plugin.shell.client.mega.stream.ShellStreamSink;
import com.sandpolis.plugin.shell.client.mega.stream.ShellStreamSource;

public final class ShellExe extends Exelet {

	@Auth
	@Handler(tag = ShellMSG.RQ_EXECUTE_FIELD_NUMBER)
	public static MessageOrBuilder rq_execute(RQ_Execute rq) throws Exception {

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
		case ZSH:
			command = Shells.ZSH.buildCommand(rq.getCommand());
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
	@Handler(tag = ShellMSG.RQ_LIST_SHELLS_FIELD_NUMBER)
	public static MessageOrBuilder rq_list_shells(RQ_ListShells rq) throws Exception {
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
		if (Shells.ZSH.getLocation() != null) {
			rs.addListing(ShellListing.newBuilder().setType(Shell.ZSH).setLocation(Shells.ZSH.getLocation()));
		}

		return rs;
	}

	@Auth
	@Handler(tag = ShellMSG.RQ_POWER_CHANGE_FIELD_NUMBER)
	public static void rq_power_change(RQ_PowerChange rq) throws Exception {
		// TODO check permissions
		// TODO avoid switches
		switch (SystemUtil.OS_TYPE) {
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

	@Auth
	@Handler(tag = ShellMSG.RQ_SHELL_STREAM_FIELD_NUMBER)
	public static MessageOrBuilder rq_shell_stream(ExeletContext context, RQ_ShellStream rq) throws Exception {
		var outcome = begin();

		ProcessBuilder session = null;
		switch (rq.getType()) {
		case BASH:
			if (Shells.BASH.getLocation() != null)
				session = new ProcessBuilder(Shells.BASH.buildSession());
			else
				return failure(outcome);
			break;
		case ZSH:
			if (Shells.ZSH.getLocation() != null)
				session = new ProcessBuilder(Shells.ZSH.buildSession());
			else
				return failure(outcome);
			break;
		case CMD:
			if (Shells.CMD.getLocation() != null)
				session = new ProcessBuilder(Shells.CMD.buildSession());
			else
				return failure(outcome);
			break;
		case PWSH:
			if (Shells.PWSH.getLocation() != null)
				session = new ProcessBuilder(Shells.PWSH.buildSession());
			else
				return failure(outcome);
			break;
		default:
			return failure(outcome);
		}

		session.redirectErrorStream(true);

		// Set default environment
		session.environment().put("TERM", "screen-256color");

		// Set initial size
		session.environment().put("COLS", rq.getCols() == 0 ? "80" : String.valueOf(rq.getCols()));
		session.environment().put("LINES", rq.getRows() == 0 ? "120" : String.valueOf(rq.getRows()));

		// Override environment
		for (var entry : rq.getEnvironmentMap().entrySet()) {
			session.environment().put(entry.getKey(), entry.getValue());
		}

		// Launch new process
		var process = session.start();

		var source = new ShellStreamSource(process);
		var sink = new ShellStreamSink(process);

		var inbound = new InboundStreamAdapter<EV_ShellStream>(rq.getId(), context.connector, ev -> {
			try {
				return ev.getPlugin().unpack(ShellMSG.class).getEvShellStream();
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		});
		var outbound = new OutboundStreamAdapter<EV_ShellStream>(rq.getId(), context.connector,
				context.request.getFrom(), ev -> {
					return Any.pack(ShellMSG.newBuilder().setEvShellStream(ev).build(), "com.sandpolis.plugin.shell");
				});

		StreamStore.add(inbound, sink);
		StreamStore.add(source, outbound);
		source.start();

		return success(outcome);
	}

	@Auth
	@Handler(tag = ShellMSG.EV_SHELL_STREAM_FIELD_NUMBER)
	public static void ev_shell_stream(ExeletContext context, EV_ShellStream ev) {
		StreamStore.streamData(context.request.getId(), ev);
	}

	private ShellExe() {
	}
}
