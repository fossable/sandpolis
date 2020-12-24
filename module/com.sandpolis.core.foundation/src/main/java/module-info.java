//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
open module com.sandpolis.core.foundation {
	exports com.sandpolis.core.foundation.idle;
	exports com.sandpolis.core.foundation.logging;
	exports com.sandpolis.core.foundation.soi;
	exports com.sandpolis.core.foundation.util;
	exports com.sandpolis.core.foundation;

	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires com.google.common;
	requires com.google.protobuf;
	requires java.prefs;
	requires java.xml;
	requires org.fusesource.jansi;
	requires org.slf4j;

	provides ch.qos.logback.classic.spi.Configurator
			with com.sandpolis.core.foundation.logging.DefaultLogbackConfigurator;
}
