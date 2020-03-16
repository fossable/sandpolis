//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.plugin.device.client.mega.snmp.library;

import java.util.function.Function;

import org.snmp4j.smi.Variable;

import com.sandpolis.plugin.device.client.mega.snmp.library.mib.SNMPv2_MIB;

public class EdgeOS implements SNMPv2_MIB {

	Function<Variable, Integer> hrSWRunPerfMem = (variable) -> {
		return variable.toInt() * 1000;
	};
}
