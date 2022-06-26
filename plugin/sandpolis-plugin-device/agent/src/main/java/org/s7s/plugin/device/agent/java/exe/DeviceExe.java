//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.exe;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.MessageLiteOrBuilder;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.plugin.device.agent.java.arp.ArpScan;
import org.s7s.plugin.device.agent.java.ipmi.IpmiScan;
import org.s7s.plugin.device.agent.java.snmp.SnmpScan;
import org.s7s.plugin.device.agent.java.ssh.SshScan;
import org.s7s.plugin.device.Messages.RQ_FindSubagents;
import org.s7s.plugin.device.Messages.RQ_FindSubagents.CommunicatorType;
import org.s7s.plugin.device.Messages.RS_FindSubagents;

public final class DeviceExe extends Exelet {

//	@Handler(auth = true)
//	public static MessageLiteOrBuilder rq_register_device(RQ_RegisterDevice rq) throws Exception {
//		// TODO
//		return null;
//	}

	@Handler(auth = true)
	public static MessageLiteOrBuilder rq_find_subagents(RQ_FindSubagents rq) throws Exception {

		var rs = RS_FindSubagents.newBuilder();

		// Determine networks to scan
		List<NetworkInterface> networks = new ArrayList<>();
		if (rq.getNetworkCount() == 0) {
			networks = NetworkInterface.networkInterfaces().collect(Collectors.toList());
		} else {
			for (String name : rq.getNetworkList()) {
				var netIf = NetworkInterface.getByName(name);
				if (netIf != null) {
					networks.add(netIf);
				}
			}
		}

		// Find hosts with ARP scan
		for (var networkInterface : networks) {
			for (var host : ArpScan.scanNetwork(networkInterface)) {
				if (rq.getCommunicatorList().contains(CommunicatorType.SSH)) {
					SshScan.scanHost(host.ip()).ifPresent(info -> {
						rs.addSshDevice(RS_FindSubagents.SshDevice.newBuilder().setIpAddress(host.ip())
								.setFingerprint(info.ssh_banner()).setFingerprint(info.fingerprint()));
					});
				}

				if (rq.getCommunicatorList().contains(CommunicatorType.SNMP)) {
					SnmpScan.scanHost(host.ip()).ifPresent(info -> {
						rs.addSnmpDevice(RS_FindSubagents.SnmpDevice.newBuilder().setIpAddress(host.ip()));
					});
				}

				if (rq.getCommunicatorList().contains(CommunicatorType.IPMI)) {
					IpmiScan.scanHost(host.ip()).ifPresent(info -> {
						rs.addIpmiDevice(RS_FindSubagents.IpmiDevice.newBuilder().setIpAddress(host.ip()));
					});
				}
			}
		}

		return rs;
	}

	private DeviceExe() {
	}
}
