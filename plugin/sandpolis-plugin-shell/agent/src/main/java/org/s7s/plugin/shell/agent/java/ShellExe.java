//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.shell.agent.java;

import static org.s7s.core.instance.stream.StreamStore.StreamStore;

import java.io.InputStreamReader;

import com.google.common.io.CharStreams;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;
import org.s7s.core.instance.stream.InboundStreamAdapter;
import org.s7s.core.instance.stream.OutboundStreamAdapter;
import org.s7s.plugin.shell.Messages.EV_ShellStreamInput;
import org.s7s.plugin.shell.Messages.EV_ShellStreamOutput;
import org.s7s.plugin.shell.Messages.RQ_Execute;
import org.s7s.plugin.shell.Messages.RQ_ListShells;
import org.s7s.plugin.shell.Messages.RQ_ShellStream;
import org.s7s.plugin.shell.Messages.RS_Execute;
import org.s7s.plugin.shell.Messages.RS_ListShells;
import org.s7s.plugin.shell.Messages.RS_ListShells.DiscoveredShell;
import org.s7s.plugin.shell.Messages.RS_ShellStream;
import org.s7s.plugin.shell.agent.java.stream.ShellStreamSink;
import org.s7s.plugin.shell.agent.java.stream.ShellStreamSource;

public final class ShellExe extends Exelet {

	@Handler(auth = true)
	public static RS_Execute rq_execute(RQ_Execute rq) throws Exception {

		Process p = Shell.of(rq.getShellPath()).execute(rq.getCommand()).start();
		try (var in = new InputStreamReader(p.getInputStream())) {
			int exit = p.waitFor();
			return RS_Execute.newBuilder().setStdout(CharStreams.toString(in)).setExitCode(exit).build();
		}
	}

	@Handler(auth = true)
	public static RS_ListShells rq_list_shells(RQ_ListShells rq) throws Exception {
		var rs = RS_ListShells.newBuilder();

		for (var shell : Shell.discoverShells()) {
			rs.addShell(DiscoveredShell.newBuilder().setLocation(shell.executable().toString())
					.setVersion(shell.version()).addAllCapability(shell.capabilities()));
		}

		return rs.build();
	}

	@Handler(auth = true)
	public static RS_ShellStream rq_shell_stream(ExeletContext context, RQ_ShellStream rq) throws Exception {

		ProcessBuilder session = Shell.of(rq.getPath()).newSession();

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

		var inbound = new InboundStreamAdapter<EV_ShellStreamInput>(rq.getStreamId(), context.connector,
				EV_ShellStreamInput.class);
		var outbound = new OutboundStreamAdapter<EV_ShellStreamOutput>(rq.getStreamId(), context.connector,
				context.request.getFrom());

		StreamStore.add(inbound, sink);
		StreamStore.add(source, outbound);
		source.start();

		return RS_ShellStream.SHELL_STREAM_OK;
	}

	private ShellExe() {
	}
}
