//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.foundation.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.fusesource.jansi.Ansi.ansi;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * Text processing utilities.
 *
 * @since 5.0.0
 */
public final class TextUtil {

	/**
	 * The default units (in descending order) in which to format durations.
	 */
	private static final ChronoUnit[] DEFAULT_DURATION_UNITS = new ChronoUnit[] { ChronoUnit.DAYS, ChronoUnit.HOURS,
			ChronoUnit.MINUTES, ChronoUnit.SECONDS };

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
	// Credit to https://stackoverflow.com/a/3758880
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
	// Credit to https://stackoverflow.com/a/3758880
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
	 * Format the given time duration in English according to the given units.
	 *
	 * @param value The duration to format
	 * @param units The units
	 * @return A formatted string
	 */
	public static String formatDuration(Duration value, ChronoUnit... units) {
		if (units.length == 0) {
			// Default units
			units = DEFAULT_DURATION_UNITS;
		} else {
			// Sort input units
			Arrays.sort(units, (u1, u2) -> u2.compareTo(u1));
		}

		// Shortcut for nil duration
		if (value.isZero()) {
			return "0 " + units[units.length - 1];
		}

		var buffer = new StringBuffer();

		// Compute the number of times each unit fits into the value
		long[] u = new long[units.length];
		for (int i = 0; i < units.length; i++) {
			if (value.compareTo(units[i].getDuration()) >= 0) {
				u[i] = value.dividedBy(units[i].getDuration());
				value = value.minus(units[i].getDuration().multipliedBy(u[i]));
			}
		}

		// Assemble the output text
		for (int i = 0; i < units.length; i++) {

			// Append value and unit if greater than 0
			if (u[i] == 1) {
				buffer.append(u[i]);
				buffer.append(' ');
				// TODO singular unit
				buffer.append(units[i]);
			} else if (u[i] > 1) {
				buffer.append(u[i]);
				buffer.append(' ');
				buffer.append(units[i]);
			} else {
				continue;
			}

			// Append a comma if there's more to come
			for (int j = i + 1; j < units.length; j++) {
				if (u[j] > 0) {
					buffer.append(", ");
					break;
				}
			}
		}

		return buffer.toString();
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
	 * Extract the version number out of the JVM --version text.
	 * 
	 * @param versionText The version info
	 * @return The version number
	 */
	public static String parseJavaVersion(String versionText) {
		checkNotNull(versionText);

		var matcher = Pattern.compile("\\b([0-9]+\\.[0-9]\\.[0-9])\\b").matcher(versionText);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	/**
	 * Compare two semantic versions.
	 * 
	 * @param version1 First version
	 * @param version2 Second version
	 * @return
	 */
	public static int compareVersion(String version1, String version2) {
		return Arrays.compare(Arrays.stream(version1.split("\\.|\\+")).mapToInt(Integer::parseInt).toArray(),
				Arrays.stream(version2.split("\\.")).mapToInt(Integer::parseInt).toArray());
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

	/**
	 * Get the value represented by the given byte count string formatted in base-2
	 * prefixes.
	 *
	 * @param count The byte count
	 * @return The value
	 */
	public static long unformatByteCount(String count) {
		String[] c = Objects.requireNonNull(count).trim().split("\\s+");
		if (c.length != 2)
			throw new IllegalArgumentException("Invalid format");

		double value = Double.parseDouble(c[0]);

		return (long) (value * switch (c[1].toLowerCase()) {
		case "b" -> 1L;
		case "kib", "kb" -> 1L << 10;
		case "mib", "mb" -> 1L << 20;
		case "gib", "gb" -> 1L << 30;
		case "tib", "tb" -> 1L << 40;
		case "pib", "pb" -> 1L << 50;
		case "eib", "eb" -> 1L << 60;
		default -> throw new IllegalArgumentException("Unknown unit: " + c[1]);
		});
	}

	/**
	 * Get the value represented by the given byte count string formatted in base-10
	 * prefixes.
	 *
	 * @param count The byte count
	 * @return The value
	 */
	public static long unformatByteCountSI(String count) {
		String[] c = Objects.requireNonNull(count).trim().split("\\s+");
		if (c.length != 2)
			throw new IllegalArgumentException("Invalid format");

		double value = Double.parseDouble(c[0]);

		return (long) (value * switch (c[1].toLowerCase()) {
		case "b" -> 1L;
		case "kb" -> 1000L;
		case "mb" -> 1000000L;
		case "gb" -> 1000000000L;
		case "tb" -> 1000000000000L;
		case "pb" -> 1000000000000000L;
		case "eb" -> 1000000000000000000L;
		default -> throw new IllegalArgumentException("Unknown unit: " + c[1]);
		});
	}

	private TextUtil() {
	}
}
