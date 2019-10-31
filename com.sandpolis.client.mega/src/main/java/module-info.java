module com.sandpolis.client.mega {
	exports com.sandpolis.client.mega.cmd;
	exports com.sandpolis.client.mega.exe;
	exports com.sandpolis.client.mega;

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
	requires transitive io.netty.common;
	requires transitive io.netty.transport;
	requires transitive org.slf4j;
}
