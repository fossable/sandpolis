open module com.sandpolis.core.profile {
	exports com.sandpolis.core.profile.attribute.key;
	exports com.sandpolis.core.profile.attribute;
	exports com.sandpolis.core.profile.store;
	exports com.sandpolis.core.profile;

	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.stream;
	requires com.sandpolis.core.util;
	requires java.persistence;
	requires org.slf4j;
}
