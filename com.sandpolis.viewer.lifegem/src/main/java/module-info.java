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
open module com.sandpolis.viewer.lifegem {
	exports com.sandpolis.viewer.lifegem.common.button;
	exports com.sandpolis.viewer.lifegem.common.controller;
	exports com.sandpolis.viewer.lifegem.common.field;
	exports com.sandpolis.viewer.lifegem.common.label;
	exports com.sandpolis.viewer.lifegem.common.pane;
	exports com.sandpolis.viewer.lifegem.common.tray;
	exports com.sandpolis.viewer.lifegem.common;
	exports com.sandpolis.viewer.lifegem.stage;
	exports com.sandpolis.viewer.lifegem.view.about;
	exports com.sandpolis.viewer.lifegem.view.generator.config_tree;
	exports com.sandpolis.viewer.lifegem.view.generator.detail;
	exports com.sandpolis.viewer.lifegem.view.generator;
	exports com.sandpolis.viewer.lifegem.view.login.phase;
	exports com.sandpolis.viewer.lifegem.view.login;
	exports com.sandpolis.viewer.lifegem.view.main.graph;
	exports com.sandpolis.viewer.lifegem.view.main.list;
	exports com.sandpolis.viewer.lifegem.view.main.menu;
	exports com.sandpolis.viewer.lifegem.view.main;
	exports com.sandpolis.viewer.lifegem;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.viewer;
	requires com.sandpolis.core.cv;
	requires fxgraph;
	requires io.netty.common;
	requires io.netty.transport;
	requires java.desktop;
	requires java.management;
	requires java.xml;
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires org.slf4j;
	requires com.sandpolis.core.sv;
}
