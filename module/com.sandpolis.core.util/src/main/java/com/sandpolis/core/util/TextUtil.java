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
package com.sandpolis.core.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.Arrays;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * Utilities for text processing.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class TextUtil {

	/**
	 * Colors available to {@link #rainbowText(String)}.
	 */
	private static final Color[] RAINBOW = Arrays.copyOfRange(Color.values(), 1, 7);

	/**
	 * Format the given byte count with base 2 unit prefixes.
	 *
	 * @param bytes The byte count
	 * @return A formatted String
	 */
	// Thanks to https://stackoverflow.com/a/3758880
	public static String formatByteCount(long bytes) {

		long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		return b < 1024L ? bytes + " B"
				: b <= 0xfffccccccccccccL >> 40 ? String.format("%.1f KiB", bytes / 0x1p10)
						: b <= 0xfffccccccccccccL >> 30 ? String.format("%.1f MiB", bytes / 0x1p20)
								: b <= 0xfffccccccccccccL >> 20 ? String.format("%.1f GiB", bytes / 0x1p30)
										: b <= 0xfffccccccccccccL >> 10 ? String.format("%.1f TiB", bytes / 0x1p40)
												: b <= 0xfffccccccccccccL
														? String.format("%.1f PiB", (bytes >> 10) / 0x1p40)
														: String.format("%.1f EiB", (bytes >> 20) / 0x1p40);
	}

	/**
	 * Format the given byte count with base 10 unit prefixes.
	 *
	 * @param bytes The byte count
	 * @return A formatted String
	 */
	// Thanks to https://stackoverflow.com/a/3758880
	public static String formatByteCountSI(long bytes) {

		long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		return b < 1000L ? bytes + " B"
				: b < 999_950L ? String.format("%.1f kB", b / 1e3)
						: (b /= 1000) < 999_950L ? String.format("%.1f MB", b / 1e3)
								: (b /= 1000) < 999_950L ? String.format("%.1f GB", b / 1e3)
										: (b /= 1000) < 999_950L ? String.format("%.1f TB", b / 1e3)
												: (b /= 1000) < 999_950L ? String.format("%.1f PB", b / 1e3)
														: String.format("%.1f EB", b / 1e6);
	}

	/**
	 * Get ascii art that says "sandpolis".
	 *
	 * @return A String of 6 lines that each measure 43 characters wide
	 */
	public static String getSandpolisArt() {
		return "                     _             _ _     \n ___  __ _ _ __   __| |_ __   ___ | (_)___ \n/ __|/ _` | '_ \\ / _` | '_ \\ / _ \\| | / __|\n\\__ \\ (_| | | | | (_| | |_) | (_) | | \\__ \\\n|___/\\__,_|_| |_|\\__,_| .__/ \\___/|_|_|___/\n                      |_|                  ";
	}

	/**
	 * Randomly colorize a String with ANSI escape codes. No two consecutive
	 * characters will be the same color.
	 *
	 * @param text The text to colorize
	 * @return The colorized text
	 */
	public static String rainbowText(String text) {
		checkNotNull(text);

		Ansi ansi = ansi(text.length()).bold();

		Color last = null;
		for (int i = 0; i < text.length(); i++) {
			Color rand = RandUtil.nextItem(RAINBOW);
			if (rand == last) {
				i--;
				continue;
			}

			last = rand;
			ansi.fg(rand).a(text.charAt(i));
		}

		return ansi.reset().toString();
	}

	private TextUtil() {
	}
}