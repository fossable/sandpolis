//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device.agent.java.snmp.library.mib;

import java.util.function.Function;

import org.snmp4j.smi.Variable;

public interface SNMPv2_MIB {

	public @interface SNMP {
		String value();
	}

	@SNMP("1.3.6.1.2.1.1.1.0")
	Function<Variable, String> sysDescr = Variable::toString;

	@SNMP("1.3.6.1.2.1.1.3.0")
	Function<Variable, Integer> sysUpTime = result -> {
		return result.toInt() * 100;
	};

}
