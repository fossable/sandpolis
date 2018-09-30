module com.sandpolis.server {
	exports com.sandpolis.server.stream;
	exports com.sandpolis.server.auth;
	exports com.sandpolis.server.store.server;
	exports com.sandpolis.server.net.init;
	exports com.sandpolis.server.gen.generator;
	exports com.sandpolis.server.store.listener;
	exports com.sandpolis.server.exe;
	exports com.sandpolis.server.gen.packager;
	exports com.sandpolis.server.store.user;
	exports com.sandpolis.server.gen;
	exports com.sandpolis.server.store.group;
	exports com.sandpolis.server;
	exports com.sandpolis.server.net.handler;

	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.ipc;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.profile;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires java.desktop;
	requires java.persistence;
	requires java.prefs;
	requires org.junit.jupiter.api;
	requires protobuf.java;
	requires slf4j.api;
	requires zt.zip;
}