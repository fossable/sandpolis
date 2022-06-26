//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.snmp.library;

import java.util.function.Function;

import org.snmp4j.smi.Variable;

import org.s7s.plugin.device.agent.java.snmp.library.mib.SNMPv2_MIB;

public class EdgeOS implements SNMPv2_MIB {

	Function<Variable, Integer> hrSWRunPerfMem = (variable) -> {
		return variable.toInt() * 1000;
	};
}
