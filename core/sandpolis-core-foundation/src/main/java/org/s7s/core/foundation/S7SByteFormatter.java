//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import java.util.Objects;

public record S7SByteFormatter(long bytes) {

	public static S7SByteFormatter of(long bytes) {
		return new S7SByteFormatter(bytes);
	}

	public static S7SByteFormatter of(double bytes) {
		return new S7SByteFormatter((long) bytes);
	}

	public static S7SByteFormatter of(String count) {
		String[] c = Objects.requireNonNull(count).trim().split("\\s+");
		if (c.length != 2)
			throw new IllegalArgumentException("Invalid format");

		double value = Double.parseDouble(c[0]);

		return of(value * switch (c[1].toLowerCase()) {
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
	 * @param count
	 * @return
	 */
	public static S7SByteFormatter ofSI(String count) {
		String[] c = Objects.requireNonNull(count).trim().split("\\s+");
		if (c.length != 2)
			throw new IllegalArgumentException("Invalid format");

		double value = Double.parseDouble(c[0]);

		return of(value * switch (c[1].toLowerCase()) {
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

	/**
	 * Format the given byte count with base 2 unit prefixes.
	 *
	 * @return A formatted String
	 */
	// Credit to https://stackoverflow.com/a/3758880
	public String humanReadable() {

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
	 * @return A formatted String
	 */
	// Credit to https://stackoverflow.com/a/3758880
	public String humanReadableSI() {

		long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		return b < 1000L ? bytes + " B"
				: b < 999_950L ? String.format("%.1f kB", b / 1e3)
						: (b /= 1000) < 999_950L ? String.format("%.1f MB", b / 1e3)
								: (b /= 1000) < 999_950L ? String.format("%.1f GB", b / 1e3)
										: (b /= 1000) < 999_950L ? String.format("%.1f TB", b / 1e3)
												: (b /= 1000) < 999_950L ? String.format("%.1f PB", b / 1e3)
														: String.format("%.1f EB", b / 1e6);
	}
}
