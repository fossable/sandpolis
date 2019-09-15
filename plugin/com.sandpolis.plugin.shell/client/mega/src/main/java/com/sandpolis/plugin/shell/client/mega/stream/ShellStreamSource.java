package com.sandpolis.plugin.shell.client.mega.stream;

import java.io.IOException;

public class ShellStreamSource {// extends StreamSource<ShellStreamData> {

	private Process process;

	public void write(String in) throws IOException {
		process.getOutputStream().write(in.getBytes());
		process.getOutputStream().flush();
	}
}
