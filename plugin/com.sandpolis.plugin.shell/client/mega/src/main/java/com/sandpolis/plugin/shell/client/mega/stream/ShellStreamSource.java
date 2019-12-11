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
package com.sandpolis.plugin.shell.client.mega.stream;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import com.google.protobuf.ByteString;
import com.sandpolis.core.stream.store.StreamSource;
import com.sandpolis.plugin.shell.net.MsgShell.EV_ShellStream;

public class ShellStreamSource extends StreamSource<EV_ShellStream> {

	private Process process;

	private Thread thread = new Thread(() -> {
		byte[] buffer = new byte[8192];
		int read;

		try (var out = process.getInputStream()) {
			while (!Thread.currentThread().isInterrupted()) {
				while ((read = out.read(buffer, 0, 8192)) >= 0) {
					submit(EV_ShellStream.newBuilder().setData(ByteString.copyFrom(buffer, 0, read)).build());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	});

	public ShellStreamSource(Process process) {
		checkArgument(process.isAlive());
		this.process = process;
	}

	@Override
	public void stop() {
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
