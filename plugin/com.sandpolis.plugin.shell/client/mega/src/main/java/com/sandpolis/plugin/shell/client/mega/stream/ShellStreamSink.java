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

import com.sandpolis.core.stream.store.StreamSink;
import com.sandpolis.plugin.shell.net.MsgShell.EV_ShellStream;

public class ShellStreamSink extends StreamSink<EV_ShellStream> {

	private Process process;

	public ShellStreamSink(Process process) {
		checkArgument(process.isAlive());
		this.process = process;
	}

	@Override
	public void onNext(EV_ShellStream item) {
		try {
			process.getOutputStream().write(item.getData().toByteArray());
			process.getOutputStream().flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
