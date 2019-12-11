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
open module com.sandpolis.core.stream {
	exports com.sandpolis.core.stream.store;
	exports com.sandpolis.core.stream;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires org.slf4j;
}
