open module com.sandpolis.core.instance {
	exports com.sandpolis.core.instance.event;
	exports com.sandpolis.core.instance.idle;
	exports com.sandpolis.core.instance.plugin;
	exports com.sandpolis.core.instance.storage.database.converter;
	exports com.sandpolis.core.instance.storage.database;
	exports com.sandpolis.core.instance.storage;
	exports com.sandpolis.core.instance.store.database;
	exports com.sandpolis.core.instance.store.plugin;
	exports com.sandpolis.core.instance.store.pref;
	exports com.sandpolis.core.instance.store.thread;
	exports com.sandpolis.core.instance.store;
	exports com.sandpolis.core.instance.util;
	exports com.sandpolis.core.instance;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.soi;
	requires com.sandpolis.core.util;
	requires java.persistence;
	requires java.prefs;
	requires org.slf4j;
	
	uses com.sandpolis.core.instance.plugin.SandpolisPlugin;
}
