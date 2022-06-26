//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static com.google.common.base.Preconditions.checkArgument;

public record S7SMacAddress(String string, byte[] bytes) {

	/**
	 * The typical broadcast address.
	 */
	public static final S7SMacAddress BROADCAST = of("FF:FF:FF:FF:FF:FF");

	/**
	 * The zero address.
	 */
	public static final S7SMacAddress ZERO = of("00:00:00:00:00:00");

	public static S7SMacAddress of(String mac) {
		var bytes = mac.split(":");
		checkArgument(bytes.length == 6);

		return new S7SMacAddress(mac,
				new byte[] { Short.valueOf(bytes[0], 16).byteValue(), Short.valueOf(bytes[1], 16).byteValue(),
						Short.valueOf(bytes[2], 16).byteValue(), Short.valueOf(bytes[3], 16).byteValue(),
						Short.valueOf(bytes[4], 16).byteValue(), Short.valueOf(bytes[5], 16).byteValue() });
	}

	public static S7SMacAddress of(byte[] mac) {
		checkArgument(mac.length == 6);

		return new S7SMacAddress(
				String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]), mac);
	}
}
