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
open module com.sandpolis.client.lifegem {
	exports com.sandpolis.client.lifegem.common.button;
	exports com.sandpolis.client.lifegem.common.controller;
	exports com.sandpolis.client.lifegem.common.field;
	exports com.sandpolis.client.lifegem.common.label;
	exports com.sandpolis.client.lifegem.common.pane;
	exports com.sandpolis.client.lifegem.common.tray;
	exports com.sandpolis.client.lifegem.common;
	exports com.sandpolis.client.lifegem.stage;
	exports com.sandpolis.client.lifegem.view.about;
	exports com.sandpolis.client.lifegem.view.generator.config_tree;
	exports com.sandpolis.client.lifegem.view.generator.detail;
	exports com.sandpolis.client.lifegem.view.generator;
	exports com.sandpolis.client.lifegem.view.login.phase;
	exports com.sandpolis.client.lifegem.view.login;
	exports com.sandpolis.client.lifegem.view.main.graph;
	exports com.sandpolis.client.lifegem.view.main.list;
	exports com.sandpolis.client.lifegem.view.main.menu;
	exports com.sandpolis.client.lifegem.view.main;
	exports com.sandpolis.client.lifegem;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.clientagent;
	requires com.sandpolis.core.clientserver;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.client;
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
}
