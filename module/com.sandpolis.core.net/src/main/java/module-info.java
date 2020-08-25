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
	exports com.sandpolis.core.net.channel.client;
	exports com.sandpolis.core.net.channel;
	exports com.sandpolis.core.net.cmdlet;
	exports com.sandpolis.core.net.connection;
	exports com.sandpolis.core.net.cvid;
	exports com.sandpolis.core.net.exception;
	exports com.sandpolis.core.net.exelet;
	exports com.sandpolis.core.net.channel.peer;
	exports com.sandpolis.core.net.handler;
	exports com.sandpolis.core.net.message;
	exports com.sandpolis.core.net.msg;
	exports com.sandpolis.core.net.network;
	exports com.sandpolis.core.net.plugin;
	exports com.sandpolis.core.net.state;
	exports com.sandpolis.core.net.stream;
	exports com.sandpolis.core.net.util;
	exports com.sandpolis.core.net;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires io.netty.buffer;
	requires io.netty.codec.dns;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.resolver.dns;
	requires io.netty.transport;
	requires org.slf4j;
}
