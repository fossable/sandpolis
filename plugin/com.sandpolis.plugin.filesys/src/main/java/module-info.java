//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
module com.sandpolis.plugin.filesys {
	exports com.sandpolis.plugin.filesys.cmd;
	exports com.sandpolis.plugin.filesys.msg;
	exports com.sandpolis.plugin.filesys.util;
	exports com.sandpolis.plugin.filesys;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires java.desktop;
	requires org.slf4j;
}
