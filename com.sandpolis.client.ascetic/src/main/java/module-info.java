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
open module com.sandpolis.client.ascetic {
	exports com.sandpolis.client.ascetic.component;
	exports com.sandpolis.client.ascetic.view.about;
	exports com.sandpolis.client.ascetic.view.control;
	exports com.sandpolis.client.ascetic.view.log;
	exports com.sandpolis.client.ascetic.view.login;
	exports com.sandpolis.client.ascetic.view.main.hosts;
	exports com.sandpolis.client.ascetic.view.main;
	exports com.sandpolis.client.ascetic;

	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires com.google.protobuf;
	requires com.googlecode.lanterna;
	requires com.sandpolis.core.clientserver;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.client;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires org.slf4j;

	provides ch.qos.logback.classic.spi.Configurator with com.sandpolis.client.ascetic.logging.LoggingConfigurator;
}
