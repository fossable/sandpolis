module com.sandpolis.core.viewer {
	exports com.sandpolis.core.viewer.cmd;
	exports com.sandpolis.core.viewer.stream;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.ipc;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.stream;
	requires com.sandpolis.core.util;
}
