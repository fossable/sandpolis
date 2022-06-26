//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.plugin.desktop.agent.java {
	exports org.s7s.plugin.desktop.agent.java.exe;
	exports org.s7s.plugin.desktop.agent.java;

	requires com.google.protobuf;
	requires org.s7s.core.foundation;
	requires org.s7s.core.instance;
	requires org.s7s.plugin.desktop;
	requires java.desktop;

	provides org.s7s.core.instance.plugin.SandpolisPlugin with org.s7s.plugin.desktop.agent.java.DesktopPlugin;
}
