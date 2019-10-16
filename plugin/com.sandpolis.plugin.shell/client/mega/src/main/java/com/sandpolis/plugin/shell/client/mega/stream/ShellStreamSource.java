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
		thread.start();
	}
}
