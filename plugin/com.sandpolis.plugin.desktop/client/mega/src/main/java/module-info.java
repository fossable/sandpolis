module com.sandpolis.plugin.desktop.client.mega {
	exports com.sandpolis.plugin.desktop.client.mega.exe;
	exports com.sandpolis.plugin.desktop.client.mega;

	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.stream;
	requires com.sandpolis.core.util;
	requires com.sandpolis.plugin.desktop;
	requires java.desktop;
	
	provides com.sandpolis.core.instance.plugin.SandpolisPlugin with com.sandpolis.plugin.desktop.client.mega.DesktopPlugin;
}
