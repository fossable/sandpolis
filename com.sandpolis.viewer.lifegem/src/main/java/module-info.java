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
	exports com.sandpolis.viewer.lifegem.store.stage;
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

	requires transitive com.google.common;
	requires transitive com.sandpolis.core.instance;
	requires transitive com.sandpolis.core.ipc;
	requires transitive com.sandpolis.core.net;
	requires transitive com.sandpolis.core.profile;
	requires transitive com.sandpolis.core.proto;
	requires transitive com.sandpolis.core.soi;
	requires transitive com.sandpolis.core.util;
	requires transitive com.sandpolis.core.viewer;
	requires transitive fxgraph;
	requires transitive io.netty.common;
	requires transitive io.netty.transport;
	requires transitive java.desktop;
	requires transitive java.management;
	requires transitive java.xml;
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.fxml;
	requires transitive javafx.graphics;
	requires transitive org.slf4j;
}
