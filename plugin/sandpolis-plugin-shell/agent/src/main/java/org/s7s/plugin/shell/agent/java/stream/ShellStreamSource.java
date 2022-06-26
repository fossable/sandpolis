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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import org.s7s.core.instance.stream.StreamSource;
import org.s7s.plugin.shell.Messages.EV_ShellStreamOutput;

public class ShellStreamSource extends StreamSource<EV_ShellStreamOutput> {

	private static final Logger log = LoggerFactory.getLogger(ShellStreamSource.class);

	private Process process;

	private Thread thread = new Thread(() -> {
		byte[] buffer = new byte[8192];
		int read;

		try (var out = process.getInputStream()) {
			while (!Thread.currentThread().isInterrupted()) {
				while ((read = out.read(buffer, 0, 8192)) >= 0) {
					submit(EV_ShellStreamOutput.newBuilder().setStdout(ByteString.copyFrom(buffer, 0, read)).build());
				}
			}
		} catch (IOException e) {
			log.error("Shell stream closed", e);
			if (process.isAlive()) {
				// TODO send closure notification
			}
		}
	});

	public ShellStreamSource(Process process) {
		checkArgument(process.isAlive());
		this.process = process;
	}

	@Override
	public void close() {
		if (thread.isAlive()) {
			thread.interrupt();
		}
		if (process.isAlive()) {
			process.destroy();
		}
	}

	@Override
	public void start() {
		thread.setDaemon(true);
		thread.start();
	}
}
