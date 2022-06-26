//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.plugin.desktop.client.lifegem {
	exports org.s7s.plugin.desktop.client.lifegem;

	requires org.s7s.core.instance;
	requires org.s7s.plugin.desktop;
	requires javafx.graphics;
	requires com.google.protobuf;

	provides org.s7s.core.instance.plugin.SandpolisPlugin with org.s7s.plugin.desktop.client.lifegem.DesktopPlugin;
}
