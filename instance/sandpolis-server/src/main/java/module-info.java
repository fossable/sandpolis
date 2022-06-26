//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
module org.s7s.instance.server {
	exports org.s7s.instance.server.auth;
	exports org.s7s.instance.server.banner;
	exports org.s7s.instance.server.channel;
	exports org.s7s.instance.server.agentbuilder.generator;
	exports org.s7s.instance.server.agentbuilder.packager;
	exports org.s7s.instance.server.agentbuilder.deployer;
	exports org.s7s.instance.server.group;
	exports org.s7s.instance.server.listener;
	exports org.s7s.instance.server.location;
	exports org.s7s.instance.server.plugin;
	exports org.s7s.instance.server.proxy;
	exports org.s7s.instance.server.state;
	exports org.s7s.instance.server.stream;
	exports org.s7s.instance.server.trust;
	exports org.s7s.instance.server.user;
	exports org.s7s.instance.server.init;
	exports org.s7s.instance.server;

	requires com.fasterxml.jackson.databind;
	requires com.google.common;
	requires com.google.protobuf;
	requires org.s7s.core.foundation;
	requires org.s7s.core.instance;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires java.net.http;
	requires org.slf4j;
	requires zipset;
	requires com.eatthepath.otp;
	requires com.hierynomus.sshj;
	requires org.mongodb.driver.sync.client;
	requires org.mongodb.bson;
}
