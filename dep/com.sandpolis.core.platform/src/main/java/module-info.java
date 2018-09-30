module com.sandpolis.core.platform {
	exports com.sandpolis.core.platform;
	exports com.sandpolis.core.platform.store;

	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires org.junit.jupiter.api;
	requires protobuf.java;
	requires slf4j.api;
	requires zt.zip;
}