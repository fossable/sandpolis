//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.plugin.filesystem {
	exports org.s7s.plugin.filesystem.cmd;
	exports org.s7s.plugin.filesystem;

	requires com.google.common;
	requires com.google.protobuf;
	requires org.s7s.core.foundation;
	requires org.s7s.core.instance;
	requires org.s7s.core.integration.fuse;
	requires java.desktop;
	requires org.slf4j;
	requires jdk.incubator.foreign;
}
