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
module com.sandpolis.installer {
	exports com.sandpolis.installer.scene.main;
	exports com.sandpolis.installer.util;
	exports com.sandpolis.installer;

	requires com.sandpolis.core.soi;
	requires com.sandpolis.core.util;
	requires java.net.http;
	requires java.xml;
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires org.slf4j;
	requires qrcodegen;
	requires static mslinks;
}
