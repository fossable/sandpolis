module com.sandpolis.agent.installer.jar {
	exports com.sandpolis.agent.installer.jar;

	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.server;
	requires zipset;

	provides com.sandpolis.core.server.generator.Packager with com.sandpolis.agent.installer.JarPackager;
}