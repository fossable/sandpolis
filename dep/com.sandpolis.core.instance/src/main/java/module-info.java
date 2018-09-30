module com.sandpolis.core.instance {
	exports com.sandpolis.core.instance;
	exports com.sandpolis.core.instance.storage.database;
	exports com.sandpolis.core.instance.idle;
	exports com.sandpolis.core.instance.store.pref;
	exports com.sandpolis.core.instance.storage;
	exports com.sandpolis.core.instance.storage.database.converter;
	exports com.sandpolis.core.instance.store.database;

	requires com.google.common;
	requires com.sandpolis.core.proto;
	requires concurrentunit;
	requires java.management;
	requires java.persistence;
	requires java.prefs;
	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;
	requires org.opentest4j;
	requires protobuf.java;
	requires slf4j.api;
}