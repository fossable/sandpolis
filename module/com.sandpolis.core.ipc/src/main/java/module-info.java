module com.sandpolis.core.ipc {
	exports com.sandpolis.core.ipc.task;
	exports com.sandpolis.core.ipc;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.soi;
	requires java.prefs;
	requires org.slf4j;
}
