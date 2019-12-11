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
open module com.sandpolis.core.net {
	exports com.sandpolis.core.net.command;
	exports com.sandpolis.core.net.exception;
	exports com.sandpolis.core.net.future;
	exports com.sandpolis.core.net.handler.cvid;
	exports com.sandpolis.core.net.handler.exelet;
	exports com.sandpolis.core.net.handler.peer;
	exports com.sandpolis.core.net.handler.sand5;
	exports com.sandpolis.core.net.handler;
	exports com.sandpolis.core.net.init;
	exports com.sandpolis.core.net.loop;
	exports com.sandpolis.core.net.plugin;
	exports com.sandpolis.core.net.sock;
	exports com.sandpolis.core.net.store.connection;
	exports com.sandpolis.core.net.store.network;
	exports com.sandpolis.core.net.util;
	exports com.sandpolis.core.net;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires io.netty.buffer;
	requires io.netty.codec.dns;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.resolver.dns;
	requires io.netty.transport;
	requires org.slf4j;
}
