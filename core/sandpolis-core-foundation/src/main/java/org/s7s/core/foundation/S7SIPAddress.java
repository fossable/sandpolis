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
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public record S7SIPAddress(byte[] asBytes, String asString, int asInt) {

	public static S7SIPAddress of(String address) {
		checkNotNull(address);
		checkArgument(!address.isBlank());

		try {
			return of(InetAddress.getByName(address));
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static S7SIPAddress of(int address) {
		try {
			return of(InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(address).array()));
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static S7SIPAddress of(byte[] address) {
		try {
			return of(InetAddress.getByAddress(address));
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static S7SIPAddress of(InetAddress address) {
		return new S7SIPAddress(address.getAddress(), address.getHostAddress(),
				ByteBuffer.wrap(address.getAddress()).getInt());
	}

	/**
	 * @param networkPrefix The number of bits allocated to the network section
	 * @return The first address in the subnet
	 */
	public S7SIPAddress getFirstAddressInNetwork(int networkPrefix) {
		if (asBytes.length != 4)
			throw new UnsupportedOperationException();

		return S7SIPAddress.of(asInt & (0x80000000 >> networkPrefix - 1) + 1);

	}

	/**
	 * @param networkPrefix The number of bits allocated to the network section
	 * @return The last address in the subnet
	 */
	public S7SIPAddress getLastAddressInNetwork(int networkPrefix) {
		if (asBytes.length != 4)
			throw new UnsupportedOperationException();

		return S7SIPAddress.of((asInt | ~(0x80000000 >> networkPrefix - 1)) - 1);
	}

	public boolean isIPv4() {
		return asBytes.length == 4;
	}

	public boolean isIPv6() {
		return asBytes.length == 16;
	}

	public boolean isPrivateIPv4() {
		if (!isIPv4()) {
			throw new UnsupportedOperationException("Only supported on IPv4 addresses");
		}

		return asBytes[0] == (byte) 127 //
				|| asBytes[0] == (byte) 10//
				|| (asBytes[0] == (byte) 172 && asBytes[1] >= (byte) 16 && asBytes[1] <= (byte) 31) //
				|| (asBytes[0] == (byte) 192 && asBytes[1] == (byte) 168);
	}

}
