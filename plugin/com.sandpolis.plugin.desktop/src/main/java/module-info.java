module com.sandpolis.plugin.desktop {
	exports com.sandpolis.plugin.desktop.cmd;
	exports com.sandpolis.plugin.desktop.net;

	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
}
