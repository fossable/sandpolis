module com.sandpolis.core.ipc {
	exports com.sandpolis.core.ipc;
	exports com.sandpolis.core.ipc.store;

	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires protobuf.java;
	requires slf4j.api;
}