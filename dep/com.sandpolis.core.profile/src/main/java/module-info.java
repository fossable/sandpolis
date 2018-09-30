module com.sandpolis.core.profile {
	exports com.sandpolis.core.attribute;
	exports com.sandpolis.core.attribute.event;
	exports com.sandpolis.core.profile.comparator;
	exports com.sandpolis.core.profile;
	exports com.sandpolis.core.attribute.io;
	exports com.sandpolis.core.attribute.key;
	exports com.sandpolis.core.profile.event;
	exports com.sandpolis.core.attribute.key.device;

	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires java.persistence;
	requires org.hibernate.orm.core;
	requires org.junit.jupiter.api;
	requires protobuf.java;
	requires slf4j.api;
}