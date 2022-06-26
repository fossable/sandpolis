//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
open module org.s7s.core.instance {
	exports org.s7s.core.instance.channel.client;
	exports org.s7s.core.instance.channel.peer;
	exports org.s7s.core.instance.channel;
	exports org.s7s.core.instance.cmdlet;
	exports org.s7s.core.instance.connection;
	exports org.s7s.core.instance.exelet;
	exports org.s7s.core.instance.handler;
	exports org.s7s.core.instance.init;
	exports org.s7s.core.instance.logging;
	exports org.s7s.core.instance.message;
	exports org.s7s.core.instance.network;
	exports org.s7s.core.instance.plugin;
	exports org.s7s.core.instance.pref;
	exports org.s7s.core.instance.profile;
	exports org.s7s.core.instance.session;
	exports org.s7s.core.instance.state.oid;
	exports org.s7s.core.instance.state.st.entangled;
	exports org.s7s.core.instance.state.st;
	exports org.s7s.core.instance.state.vst;
	exports org.s7s.core.instance.state;
	exports org.s7s.core.instance.store;
	exports org.s7s.core.instance.stream;
	exports org.s7s.core.instance.thread;
	exports org.s7s.core.instance.util;
	exports org.s7s.core.instance;

	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires com.fasterxml.jackson.databind;
	requires com.google.common;
	requires com.google.protobuf;
	requires org.s7s.core.foundation;
	requires transitive org.s7s.core.protocol;
	requires io.netty.buffer;
	requires io.netty.codec.dns;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.resolver.dns;
	requires io.netty.transport;
	requires java.prefs;
	requires org.slf4j;

	uses org.s7s.core.instance.plugin.SandpolisPlugin;

	provides ch.qos.logback.classic.spi.Configurator with org.s7s.core.instance.logging.DefaultLogbackConfigurator;
}
