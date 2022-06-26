//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.plugin.desktop {
	exports org.s7s.plugin.desktop.cmd;
	exports org.s7s.plugin.desktop;

	requires com.google.protobuf;
	requires org.s7s.core.instance;
}
