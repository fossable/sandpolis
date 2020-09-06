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

	requires com.google.common;
	requires com.google.protobuf;
	requires com.fasterxml.jackson.databind;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.sv;
	requires com.sandpolis.core.cs;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires java.net.http;
	requires org.hibernate.orm.core;
	requires java.xml.bind;
	requires org.slf4j;
	requires zipset;
	requires java.persistence;
	requires static java.desktop;
}
