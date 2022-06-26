//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
open module org.s7s.instance.client.desktop {
	exports org.s7s.instance.client.desktop.ui.common.button;
	exports org.s7s.instance.client.desktop.ui.common.tray;
	exports org.s7s.instance.client.desktop.ui.common.label;
	exports org.s7s.instance.client.desktop.stage;
	exports org.s7s.instance.client.desktop.init;
	exports org.s7s.instance.client.desktop.plugin;
	exports org.s7s.instance.client.desktop.ui.common.pane;
	exports org.s7s.instance.client.desktop.ui.common;
	exports org.s7s.instance.client.desktop.ui.common.field;
	exports org.s7s.instance.client.desktop.ui.common.controller;
	exports org.s7s.instance.client.desktop;

	requires kotlin.stdlib;
	requires com.google.common;
	requires com.google.protobuf;
	requires org.s7s.core.foundation;
	requires org.s7s.core.instance;
	requires org.s7s.core.client;
	requires org.s7s.core.clientserver;
	requires io.netty.common;
	requires io.netty.transport;
	requires java.desktop;
	requires java.xml;
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires org.slf4j;
	requires tornadofx;
}
