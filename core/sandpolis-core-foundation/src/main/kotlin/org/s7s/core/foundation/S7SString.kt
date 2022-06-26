//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

public record S7SString(String text) {

	private val EMAIL_VALIDATOR = Regex(
			"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")

	private val DNS_VALIDATOR = Regex("^(?![0-9]+$)(?!-)[a-zA-Z0-9-]{,63}(?<!-)$")

	public static enum AnsiColor {
		BLUE(4), //
		CYAN(6), //
		GREEN(2), //
		MAGENTA(5), //
		RED(1), //
		YELLOW(3);

		public static String reset() {
			return String.format("\u001b[%dm", 0);
		}

		private final int value;

		AnsiColor(int index) {
			this.value = index;
		}

		public String bg() {
			return String.format("\u001b[%dm", value + 40);
		}

		public String bgBright() {
			return String.format("\u001b[%dm", value + 100);
		}

		public String fg() {
			return String.format("\u001b[%dm", value + 30);
		}

		public String fgBright() {
			return String.format("\u001b[%dm", value + 90);
		}
	}

	public static S7SString of(Object text) {
		checkNotNull(text);
		return new S7SString(text.toString());
	}

	/**
	 * @return Whether the terminal is attached to a console and supports ANSI color
	 *         escape codes.
	 */
	public static boolean checkAnsiColors() {
		return true; // TODO
	}

	/**
	 * Colorize a string with ANSI escape codes.
	 *
	 * @param color The text color
	 * @return The colorized text
	 */
	fun String.colorize(AnsiColor color) : String {

		if (!checkAnsiColors()) {
			return text;
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append(color.fg());
		buffer.append(text);
		buffer.append(AnsiColor.reset());

		return buffer.toString();
	}

	/**
	 * Randomly colorize a string with ANSI escape codes. No two consecutive
	 * characters will be the same color.
	 *
	 * @return The colorized text
	 */
	fun String.rainbowize() : String {

		if (!checkAnsiColors()) {
			return text;
		}

		AnsiColor[] colors = AnsiColor.values();

		StringBuilder buffer = new StringBuilder();

		AnsiColor last = null;
		for (int i = 0; i < text.length(); i++) {
			AnsiColor rand = S7SRandom.nextItem(colors);
			if (rand == last) {
				i--;
				continue;
			}

			last = rand;
			buffer.append(rand.fg());
			buffer.append(text.charAt(i));
		}
		buffer.append(AnsiColor.reset());

		return buffer.toString();
	}

	/**
	 * @return Whether the text is a valid IPv4 in the private address space
	 */
	fun String.isValidPrivateIPv4() : String {
		try {
			return S7SIPAddress.of(text).isPrivateIPv4();
		} catch (Exception e) {
			e.printStackTrace();
			// Slow path
			return false;
		}
	}

	/**
	 * @return Whether the text is a valid DNS name
	 */
	fun String.isValidDns() : Boolean {
		return DNS_VALIDATOR.matches(this@String)
	}

	/**
	 * @return Whether the text is a valid 2-byte port number
	 */
	fun String.isValidPort() : Boolean {
		try {
			int port = Integer.parseInt(text);
			return (port >= 0 && port < 65536);
		} catch (Exception t) {
			// Slow path
			return false;
		}
	}

	/**
	 * @return Whether the text is a valid filesystem path
	 */
	fun String.isValidPath() : Boolean {
		try {
			Paths.get(text);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * @return Whether the text is a valid email address
	 */
	fun String.isValidEmail() : Boolean {
		return EMAIL_VALIDATOR.matches(this@String)
	}

	/**
	 * @return Whether the text is a valid IPv4 address
	 */
	fun String.isValidIPv4() : Boolean {
		return IPV4_VALIDATOR.matches(this@String)
	}
}
