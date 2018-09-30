module com.sandpolis.core.net {
	exports com.sandpolis.core.net.init;
	exports com.sandpolis.core.net.exception;
	exports com.sandpolis.core.net.future;
	exports com.sandpolis.core.net.loop;
	exports com.sandpolis.core.net.store.network;
	exports com.sandpolis.core.net;
	exports com.sandpolis.core.net.codec;
	exports com.sandpolis.core.net.handler;
	exports com.sandpolis.core.net.store.connection;

	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires org.junit.jupiter.api;
	requires protobuf.java;
	requires slf4j.api;
}