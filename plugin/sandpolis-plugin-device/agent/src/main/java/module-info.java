//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.plugin.device.agent.java {
	exports org.s7s.plugin.device.agent.java.snmp.library;
	exports org.s7s.plugin.device.agent.java.arp;

	requires com.google.protobuf;
	requires org.s7s.core.instance;
	requires org.s7s.plugin.device;
	requires org.snmp4j;
	requires org.slf4j;

	requires jdk.incubator.foreign;
	requires org.s7s.core.foundation;
	requires org.s7s.core.integration.linux;
}
