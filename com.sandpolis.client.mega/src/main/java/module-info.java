module com.sandpolis.client.mega {
	exports com.sandpolis.client.mega.cmd;
	exports com.sandpolis.client.mega.module.browser.history.chrome;

	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires java.persistence;
	requires protobuf.java;
}