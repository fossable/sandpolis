module com.sandpolis.viewer {
	exports com.sandpolis.viewer;
	exports com.sandpolis.viewer.store.instance;
	exports com.sandpolis.viewer.cmd;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.ipc;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires io.netty.common;
	requires io.netty.transport;
	requires java.prefs;
	requires slf4j.api;
}