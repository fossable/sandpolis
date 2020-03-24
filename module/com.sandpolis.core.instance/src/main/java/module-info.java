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
open module com.sandpolis.core.instance {
	exports com.sandpolis.core.instance.event;
	exports com.sandpolis.core.instance.idle;
	exports com.sandpolis.core.instance.logging;
	exports com.sandpolis.core.instance.plugin;
	exports com.sandpolis.core.instance.storage.database.converter;
	exports com.sandpolis.core.instance.storage.database;
	exports com.sandpolis.core.instance.storage;
	exports com.sandpolis.core.instance.store.database;
	exports com.sandpolis.core.instance.store.plugin;
	exports com.sandpolis.core.instance.store.pref;
	exports com.sandpolis.core.instance.store.thread;
	exports com.sandpolis.core.instance.store;
	exports com.sandpolis.core.instance.util;
	exports com.sandpolis.core.instance;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.soi;
	requires com.sandpolis.core.util;
	requires java.persistence;
	requires java.prefs;
	requires org.slf4j;
	requires logback.classic;
	requires logback.core;

	uses com.sandpolis.core.instance.plugin.SandpolisPlugin;

	provides ch.qos.logback.classic.spi.Configurator
			with com.sandpolis.core.instance.logging.InstanceLoggingConfigurator;
}
