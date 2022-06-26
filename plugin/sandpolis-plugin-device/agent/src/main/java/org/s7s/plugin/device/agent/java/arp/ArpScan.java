//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.arp;

import java.net.NetworkInterface;
import java.util.Set;

import org.s7s.core.foundation.S7SSystem;

public final class ArpScan {

	public static record ArpDevice(String ip, String mac) {
	}

	public static Set<ArpDevice> scanNetwork(NetworkInterface networkInterface) throws Exception {

		// Check the network size first
		for (var address : networkInterface.getInterfaceAddresses()) {
			if (address.getNetworkPrefixLength() < 20) {
				throw new RuntimeException();
			}
		}

		switch (S7SSystem.OS_TYPE) {
		case LINUX:
			return new ArpScannerLinux(networkInterface, networkInterface.getInterfaceAddresses().get(1)).run();
		case WINDOWS:
		default:
			throw new RuntimeException();
		}
	}
}
