//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.plugin.update.agent.java {
	exports org.s7s.plugin.update.agent.java;

	requires com.google.common;
	requires com.google.protobuf;
	requires org.s7s.core.foundation;
	requires org.s7s.core.instance;
	requires org.s7s.plugin.update;
	requires org.slf4j;
}
