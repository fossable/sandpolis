module com.sandpolis.core.stream {
	exports com.sandpolis.core.stream.desktop;
	exports com.sandpolis.core.stream.attribute;
	exports com.sandpolis.core.stream.file;
	exports com.sandpolis.core.stream;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.profile;
	requires com.sandpolis.core.proto;
	requires java.persistence;
}