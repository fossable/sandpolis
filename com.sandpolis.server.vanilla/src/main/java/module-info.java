open module com.sandpolis.server.vanilla {
	exports com.sandpolis.server.vanilla.auth;
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

	requires com.sandpolis.core.storage.hibernate;
	requires org.hibernate.orm.core;
	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.ipc;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.profile;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.soi;
	requires com.sandpolis.core.stream;
	requires com.sandpolis.core.util;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires java.desktop;
	requires java.persistence;
	requires java.sql;
	requires org.slf4j;
	requires zipset;
}
