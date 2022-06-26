//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.ipmi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

public final class IpmiScan {

	public static record IpmiScanResult(String ipmi_version) {
	}

	private static record RmcpHeader(byte version, byte reserved, byte sequence_number, byte message_class) {

	}

	public static Optional<IpmiScanResult> scanHost(String ip_address) throws IOException {

		var socket = new DatagramSocket(45680);

		var packetData = ByteBuffer.allocate(0);

		socket.send(
				new DatagramPacket(packetData.array(), packetData.capacity(), new InetSocketAddress(ip_address, 623)));

		// Attempt to receive response
		var response = new DatagramPacket(new byte[0], 0);
		socket.receive(response);

		return null;
	}
}
