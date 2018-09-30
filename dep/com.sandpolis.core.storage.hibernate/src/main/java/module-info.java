module com.sandpolis.core.storage.hibernate {
	exports com.sandpolis.core.storage.hibernate;

	requires com.sandpolis.core.instance;
	requires concurrentunit;
	requires exec;
	requires java.naming;
	requires java.persistence;
	requires java.sql;
	requires mariaDB4j.core;
	requires org.hibernate.orm.core;
	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;
}