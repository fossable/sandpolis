//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.shell.agent.java.stream;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import org.s7s.core.instance.stream.StreamSink;
import org.s7s.plugin.shell.Messages.EV_ShellStreamInput;

public class ShellStreamSink extends StreamSink<EV_ShellStreamInput> {

	private Process process;

	public ShellStreamSink(Process process) {
		checkArgument(process.isAlive());
		this.process = process;
	}

	@Override
	public void onNext(EV_ShellStreamInput item) {
		if (!item.getStdin().isEmpty()) {
			try {
				process.getOutputStream().write(item.getStdin().toByteArray());
				process.getOutputStream().flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// TODO change terminal size if set
	}
}
