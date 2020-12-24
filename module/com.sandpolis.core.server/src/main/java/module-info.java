//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
module com.sandpolis.core.server {
	exports com.sandpolis.core.server.auth;
	exports com.sandpolis.core.server.banner;
	exports com.sandpolis.core.server.channel;
	exports com.sandpolis.core.server.generator;
	exports com.sandpolis.core.server.group;
	exports com.sandpolis.core.server.hibernate;
	exports com.sandpolis.core.server.listener;
	exports com.sandpolis.core.server.location;
	exports com.sandpolis.core.server.plugin;
	exports com.sandpolis.core.server.proxy;
	exports com.sandpolis.core.server.state;
	exports com.sandpolis.core.server.stream;
	exports com.sandpolis.core.server.trust;
	exports com.sandpolis.core.server.user;

	requires static java.desktop;

	requires com.fasterxml.jackson.databind;
	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.clientserver;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.serveragent;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires java.net.http;
	requires java.persistence;
	requires java.xml.bind;
	requires org.hibernate.orm.core;
	requires org.slf4j;
	requires zipset;
}
