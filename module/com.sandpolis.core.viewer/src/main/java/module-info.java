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
module com.sandpolis.core.viewer {
	exports com.sandpolis.core.viewer.cmd;
	exports com.sandpolis.core.viewer.stream;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.ipc;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.stream;
	requires com.sandpolis.core.util;
	requires com.google.common;
	requires com.sandpolis.core.soi;
}
