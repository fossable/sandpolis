module com.sandpolis.plugin.filesys.client.mega {
	exports com.sandpolis.plugin.filesys.client.mega.exe;
	exports com.sandpolis.plugin.filesys.client.mega;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.plugin.filesys;
}
