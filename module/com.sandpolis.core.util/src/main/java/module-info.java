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
open module com.sandpolis.core.util {
	exports com.sandpolis.core.util;

	requires com.google.common;
	requires java.persistence;
	requires java.xml;
	requires org.fusesource.jansi;
	requires org.slf4j;
	requires com.google.protobuf;
}
