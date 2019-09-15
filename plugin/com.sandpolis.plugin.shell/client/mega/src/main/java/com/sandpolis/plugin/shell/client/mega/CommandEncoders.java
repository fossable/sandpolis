package com.sandpolis.plugin.shell.client.mega;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CommandEncoders {

	public static final class BashEncoder {
		public static String[] encode(String command) {
			return new String[] { "sh", "-c",
					"echo " + Base64.getEncoder().encodeToString(command.getBytes()) + " | base64 --decode | sh" };
		}
	}

	public static final class PowerShellEncoder {
		public static String[] encode(String command) {
			return new String[] { "powershell", "-encodedCommand",
					Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_16LE)) };
		}
	}
}
