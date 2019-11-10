module com.sandpolis.plugin.sysinfo.client.mega {
	exports com.sandpolis.plugin.sysinfo.client.mega;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.profile;
	requires com.sandpolis.core.proto;

	provides com.sandpolis.core.instance.plugin.SandpolisPlugin with com.sandpolis.plugin.sysinfo.client.mega.SysinfoPlugin;
}
