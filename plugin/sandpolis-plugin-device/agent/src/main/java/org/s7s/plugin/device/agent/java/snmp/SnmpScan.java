//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.snmp;

import java.util.Optional;

public final class SnmpScan {

	public static record SnmpScanResult(String snmp_version) {
	}

	public static Optional<SnmpScanResult> scanHost(String ip_address) {

		// Use SNMP4j?
		return Optional.empty();
	}

}
