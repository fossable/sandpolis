module com.sandpolis.plugin.shell.client.mega {
	exports com.sandpolis.plugin.shell.client.mega.exe;
	exports com.sandpolis.plugin.shell.client.mega.shell;
	exports com.sandpolis.plugin.shell.client.mega.stream;
	exports com.sandpolis.plugin.shell.client.mega;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.stream;
	requires com.sandpolis.core.util;
	requires com.sandpolis.plugin.shell;
}
