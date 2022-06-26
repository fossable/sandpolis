//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.wol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.s7s.core.foundation.S7SMacAddress;

public record MagicPacket(DatagramPacket packet) {

	private static final int PACKET_SIZE = 6 + 6 * 6;

	public static MagicPacket of(String mac) {
		var packet = ByteBuffer.allocate(PACKET_SIZE);

		// Set the broadcast address
		packet.put(S7SMacAddress.BROADCAST.bytes());

		// Add the target MAC 6 times
		for (int i = 0; i < 6; i++) {
			packet.put(S7SMacAddress.of(mac).bytes());
		}

		try {
			return new MagicPacket(new DatagramPacket(packet.array(), PACKET_SIZE, InetAddress.getByName(""), 9));
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	public void send() throws IOException {
		try (DatagramSocket socket = new DatagramSocket()) {

			// Send the magic packet a few times
			for (int i = 0; i < 3; i++) {
				socket.send(packet);
			}
		}
	}
}
