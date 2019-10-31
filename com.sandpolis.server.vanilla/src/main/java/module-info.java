module com.sandpolis.server.vanilla {
	exports com.sandpolis.server.vanilla.auth;
	exports com.sandpolis.server.vanilla.exe;
	exports com.sandpolis.server.vanilla.gen.generator;
	exports com.sandpolis.server.vanilla.gen.packager;
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
	requires transitive com.google.common;
	requires transitive com.google.protobuf;
	requires transitive com.sandpolis.core.instance;
	requires transitive com.sandpolis.core.ipc;
	requires transitive com.sandpolis.core.net;
	requires transitive com.sandpolis.core.profile;
	requires transitive com.sandpolis.core.proto;
	requires transitive com.sandpolis.core.soi;
	requires transitive com.sandpolis.core.stream;
	requires transitive com.sandpolis.core.util;
	requires transitive io.netty.buffer;
	requires transitive io.netty.codec;
	requires transitive io.netty.common;
	requires transitive io.netty.handler;
	requires transitive io.netty.transport;
	requires transitive java.desktop;
	requires transitive java.persistence;
	requires transitive org.slf4j;
	requires transitive zipset;
}
