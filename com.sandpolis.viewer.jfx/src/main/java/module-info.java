module com.sandpolis.viewer.jfx {
	exports com.sandpolis.viewer.jfx.store.pref;
	exports com.sandpolis.viewer.jfx.common.label;
	exports com.sandpolis.viewer.jfx.view.login;
	exports com.sandpolis.viewer.jfx;
	exports com.sandpolis.viewer.jfx.view.main.menu;
	exports com.sandpolis.viewer.jfx.common;
	exports com.sandpolis.viewer.jfx.view.main;
	exports com.sandpolis.viewer.jfx.common.pane;
	exports com.sandpolis.viewer.jfx.view.about;
	exports com.sandpolis.viewer.jfx.attribute;

	requires StlMeshImporter;
	requires com.google.common;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.profile;
	requires com.sandpolis.core.proto;
	requires com.sandpolis.core.util;
	requires com.sandpolis.viewer;
	requires commons.validator;
	requires io.netty.common;
}