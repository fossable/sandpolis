module com.sandpolis.viewer.cli {
	exports com.sandpolis.viewer.cli.view.log;
	exports com.sandpolis.viewer.cli.view.control;
	exports com.sandpolis.viewer.cli;
	exports com.sandpolis.viewer.cli.view.main;
	exports com.sandpolis.viewer.cli.view.main.hosts;
	exports com.sandpolis.viewer.cli.component;
	exports com.sandpolis.viewer.cli.view.about;
	exports com.sandpolis.viewer.cli.view.login;

	requires SLPanels;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires com.sandpolis.viewer;
	requires lanterna;
	requires logback.classic;
	requires logback.core;
	requires slf4j.api;
}