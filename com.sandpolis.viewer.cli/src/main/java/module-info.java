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
module com.sandpolis.viewer.cli {
	exports com.sandpolis.viewer.cli.component;
	exports com.sandpolis.viewer.cli.view.about;
	exports com.sandpolis.viewer.cli.view.control;
	exports com.sandpolis.viewer.cli.view.log;
	exports com.sandpolis.viewer.cli.view.login;
	exports com.sandpolis.viewer.cli.view.main.hosts;
	exports com.sandpolis.viewer.cli.view.main;
	exports com.sandpolis.viewer.cli;

	requires transitive com.sandpolis.core.instance;
	requires transitive com.sandpolis.core.ipc;
	requires transitive com.sandpolis.core.net;
	requires transitive com.sandpolis.core.proto;
	requires transitive com.sandpolis.core.soi;
	requires transitive com.sandpolis.core.util;
	requires transitive com.sandpolis.core.viewer;
	requires transitive lanterna;
	requires transitive logback.classic;
	requires transitive logback.core;
	requires transitive org.slf4j;
}
