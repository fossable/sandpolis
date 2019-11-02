module com.sandpolis.core.storage.hibernate {
	exports com.sandpolis.core.storage.hibernate;

	requires com.sandpolis.core.instance;
	requires java.persistence;
	requires java.xml.bind;
	requires net.bytebuddy;
	requires org.hibernate.orm.core;
}
