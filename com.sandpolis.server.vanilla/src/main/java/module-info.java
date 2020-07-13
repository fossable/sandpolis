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
open module com.sandpolis.server.vanilla {
	exports com.sandpolis.server.vanilla.exe;
	exports com.sandpolis.server.vanilla.gen.mega;
	exports com.sandpolis.server.vanilla.gen;
	exports com.sandpolis.server.vanilla.net.handler;
	exports com.sandpolis.server.vanilla.net.init;
	exports com.sandpolis.server.vanilla.store.group;
	exports com.sandpolis.server.vanilla.store.listener;
	exports com.sandpolis.server.vanilla.store.server;
	exports com.sandpolis.server.vanilla.store.trust;
	exports com.sandpolis.server.vanilla.store.user;
	exports com.sandpolis.server.vanilla.stream;
	exports com.sandpolis.server.vanilla;
	exports com.sandpolis.server.vanilla.store.location;

	requires com.fasterxml.jackson.databind;
	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.ipc;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.foundation;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires java.desktop;
	requires java.net.http;
	requires java.persistence;
	requires java.sql;
	requires org.hibernate.orm.core;
	requires org.slf4j;
	requires zipset;
}
