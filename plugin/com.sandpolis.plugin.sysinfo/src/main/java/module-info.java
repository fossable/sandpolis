module com.sandpolis.plugin.sysinfo {
	exports com.sandpolis.plugin.sysinfo.state;

	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires com.google.common;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
}