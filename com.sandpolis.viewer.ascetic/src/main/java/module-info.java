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
open module com.sandpolis.viewer.ascetic {
	exports com.sandpolis.viewer.ascetic.component;
	exports com.sandpolis.viewer.ascetic.view.about;
	exports com.sandpolis.viewer.ascetic.view.control;
	exports com.sandpolis.viewer.ascetic.view.log;
	exports com.sandpolis.viewer.ascetic.view.login;
	exports com.sandpolis.viewer.ascetic.view.main.hosts;
	exports com.sandpolis.viewer.ascetic.view.main;
	exports com.sandpolis.viewer.ascetic;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.viewer;
	requires com.sandpolis.core.sv;
	requires com.googlecode.lanterna;
	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires org.slf4j;
	requires io.netty.transport;
	requires io.netty.common;
	requires io.netty.handler;
	requires com.google.protobuf;
	requires com.sandpolis.core.foundation;

	provides ch.qos.logback.classic.spi.Configurator with com.sandpolis.viewer.ascetic.logging.LoggingConfigurator;
}
