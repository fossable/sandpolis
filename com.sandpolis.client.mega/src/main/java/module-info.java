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
open module com.sandpolis.client.mega {
	exports com.sandpolis.client.mega.cmd;
	exports com.sandpolis.client.mega.exe;
	exports com.sandpolis.client.mega;

	requires transitive com.google.common;
	requires transitive com.google.protobuf;
	requires transitive com.sandpolis.core.instance;
	requires transitive com.sandpolis.core.ipc;
	requires transitive com.sandpolis.core.net;
	requires transitive com.sandpolis.core.soi;
	requires transitive com.sandpolis.core.util;
	requires transitive io.netty.common;
	requires transitive io.netty.transport;
	requires transitive org.slf4j;
}
