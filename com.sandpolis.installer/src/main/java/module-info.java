module com.sandpolis.installer {
	exports com.sandpolis.installer.install;
	exports com.sandpolis.installer.scene.main;
	exports com.sandpolis.installer.util;
	exports com.sandpolis.installer;

	requires com.sandpolis.core.soi;
	requires com.sandpolis.core.util;
	requires java.net.http;
	requires java.xml;
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires org.slf4j;
	requires qrcodegen;
}
