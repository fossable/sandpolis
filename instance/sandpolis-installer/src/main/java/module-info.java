//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
open module org.s7s.instance.installer.java {
	exports org.s7s.instance.installer.java.scene.main;
	exports org.s7s.instance.installer.java.util;
	exports org.s7s.instance.installer.java;

	requires org.s7s.core.foundation;
	requires org.s7s.core.integration.systemd;
	requires java.net.http;
	requires java.xml;
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires org.slf4j;
	requires io.nayuki.qrcodegen;
	requires com.fasterxml.jackson.databind;
}
