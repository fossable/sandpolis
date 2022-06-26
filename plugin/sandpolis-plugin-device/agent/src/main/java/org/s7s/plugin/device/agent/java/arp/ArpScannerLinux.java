//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.arp;

import static org.s7s.core.integration.linux.if_arp.sockaddr_ll.sll_family$set;
import static org.s7s.core.integration.linux.if_arp.sockaddr_ll.sll_halen$set;
import static org.s7s.core.integration.linux.if_arp.sockaddr_ll.sll_ifindex$set;
import static org.s7s.core.integration.linux.if_arp.sockaddr_ll.sll_pkttype$set;
import static org.s7s.core.integration.linux.socket.socket_h.bind;
import static org.s7s.core.integration.linux.socket.socket_h.recvfrom;
import static org.s7s.core.integration.linux.socket.socket_h.sendto;
import static org.s7s.core.integration.linux.socket.socket_h.socket;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.integration.linux.if_arp.sockaddr_ll;
import org.s7s.core.integration.linux.if_ether.if_ether_h;
import org.s7s.core.integration.linux.if_packet.if_packet_h;
import org.s7s.core.integration.linux.inet.inet_h;
import org.s7s.core.integration.linux.socket.socket_h;
import org.s7s.core.foundation.S7SIPAddress;
import org.s7s.plugin.device.agent.java.arp.ArpScan.ArpDevice;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

public final class ArpScannerLinux {

	private static final Logger log = LoggerFactory.getLogger(ArpScannerLinux.class);

	private static final byte[] ARP_DEST_ADDRESS = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff };

	private static final byte[] ARP_TARGET_ADDRESS = new byte[] { 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00 };

	private final ResourceScope scope = ResourceScope.newImplicitScope();

	private final NetworkInterface networkInterface;

	private final InterfaceAddress interfaceAddress;

	private int addresses;

	public ArpScannerLinux(NetworkInterface networkInterface, InterfaceAddress interfaceAddress) {
		this.networkInterface = networkInterface;
		this.interfaceAddress = interfaceAddress;
	}

	public void send_arp(int fd, byte[] dest_ip) throws SocketException {

		var sockaddr = sockaddr_ll.allocate(scope);
		sll_family$set(sockaddr, (short) socket_h.AF_PACKET());
		sll_ifindex$set(sockaddr, networkInterface.getIndex());
		sll_pkttype$set(sockaddr, (byte) if_packet_h.PACKET_BROADCAST());
		sll_halen$set(sockaddr, (byte) networkInterface.getHardwareAddress().length);

		var packet = MemorySegment.allocateNative(42, scope);

		// Write Ethernet header
		packet.asSlice(0, 14).asByteBuffer().order(ByteOrder.BIG_ENDIAN)

				// Broadcast address
				.put(ARP_DEST_ADDRESS)

				// Source address
				.put(networkInterface.getHardwareAddress())

				// EtherType
				.putShort((short) if_ether_h.ETH_P_ARP());

		// Write ARP header
		packet.asSlice(14, 28).asByteBuffer().order(ByteOrder.BIG_ENDIAN)

				// Hardware type (offset 0)
				.putShort((short) 1)

				// Protocol type (offset 2)
				.putShort((short) if_ether_h.ETH_P_IP())

				// Hardware address length (offset 4)
				.put((byte) networkInterface.getHardwareAddress().length)

				// Protocol address length (offset 5)
				.put((byte) interfaceAddress.getAddress().getAddress().length)

				// Operation type (offset 6)
				.putShort((short) 1)

				// Sender hardware address (offset 8)
				.put(networkInterface.getHardwareAddress())

				// Sender protocol address (offset 14)
				.put(interfaceAddress.getAddress().getAddress())

				// Target hardware address (offset 18)
				.put(ARP_TARGET_ADDRESS)

				// Target protocol address (offset 24)
				.put(dest_ip);

		if (sendto(fd, packet, packet.byteSize(), 0, sockaddr, (int) sockaddr_ll.sizeof()) == -1) {
			throw new RuntimeException();
		}
	}

	public int bind_arp() {

		int fd = socket(socket_h.AF_PACKET(), socket_h.SOCK_RAW(), inet_h.htons((short) if_ether_h.ETH_P_ARP()));
		if (fd < 1) {
			throw new RuntimeException("Failed to allocate raw socket");
		}

		log.debug("Allocated raw socket: {}", fd);

		var sockaddr = sockaddr_ll.allocate(scope);
		sll_family$set(sockaddr, (short) socket_h.AF_PACKET());
		sll_ifindex$set(sockaddr, networkInterface.getIndex());

		log.debug("Binding raw socket to interface: {}", networkInterface.getIndex());
		if (bind(fd, sockaddr, (int) sockaddr.byteSize()) < 0) {
			// TODO close fd
			throw new RuntimeException();
		}

		return fd;
	}

	public ArpDevice read_arp(int fd) throws Exception {

		var packet = MemorySegment.allocateNative(60, scope);

		while (true) {
			long read = recvfrom(fd, packet, packet.byteSize(), 0, MemoryAddress.NULL, MemoryAddress.NULL);
			if (read == -1) {
				throw new RuntimeException("Failed to read from raw socket");
			}

			var arp_header = packet.asSlice(14, 60 - 14).asByteBuffer();

			// Hardware type (offset 0)
			if (arp_header.getShort() != 1)
				continue;

			// Protocol type (offset 2)
			if (arp_header.getShort() != if_ether_h.ETH_P_IP())
				continue;

			// Hardware address length (offset 4)
			if (arp_header.get() != 6)
				continue;

			// Protocol address length (offset 5)
			if (arp_header.get() != 4)
				continue;

			// Operation type (offset 6)
			if (arp_header.getShort() != 2)
				continue;

			var sender_hardware_address = new byte[6];
			arp_header.get(sender_hardware_address);

			var sender_protocol_address = new byte[4];
			arp_header.get(sender_protocol_address);

			return new ArpDevice(InetAddress.getByAddress(sender_protocol_address).getHostAddress(), null);
		}
	}

	public Set<ArpDevice> run() throws Exception {

		var address = S7SIPAddress.of(interfaceAddress.getAddress());

		Set<ArpDevice> results = new HashSet<>();

		int fd = bind_arp();

		// Start read loop
		var recvThread = new Thread(() -> {
			while (!Thread.interrupted()) {
				try {
					results.add(read_arp(fd));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		recvThread.start();

		int firstAddress = address.getFirstAddressInNetwork(interfaceAddress.getNetworkPrefixLength()).asInt();
		int lastAddress = address.getLastAddressInNetwork(interfaceAddress.getNetworkPrefixLength()).asInt();
		log.debug("Preparing to scan network of size: {}", lastAddress - firstAddress);

		// Start sending
		for (int i = firstAddress; i <= lastAddress; i++) {
			var a = S7SIPAddress.of(i);
			if (!a.equals(address)) {
				send_arp(fd, a.asBytes());
			}
		}

		// Wait some time before stopping the receive thread
		Thread.sleep(1000);
		recvThread.interrupt();

		return results;
	}
}
