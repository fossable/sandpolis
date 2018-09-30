module com.sandpolis.core.storage.ormlite {
	exports com.sandpolis.core.storage.ormlite;

	requires com.sandpolis.core.instance;
	requires concurrentunit;
	requires exec;
	requires java.persistence;
	requires java.sql;
	requires mariaDB4j.core;
	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;
	requires ormlite.core;
	requires ormlite.jdbc;
}