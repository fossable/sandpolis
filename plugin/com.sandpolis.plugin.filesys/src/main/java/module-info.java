module com.sandpolis.plugin.filesys {
	exports com.sandpolis.plugin.filesys.cmd;
	exports com.sandpolis.plugin.filesys.net;
	exports com.sandpolis.plugin.filesys.util;
	exports com.sandpolis.plugin.filesys;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires java.desktop;
	requires org.slf4j;
}
