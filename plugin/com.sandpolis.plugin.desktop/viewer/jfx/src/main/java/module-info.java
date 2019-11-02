module com.sandpolis.plugin.desktop.viewer.jfx {
	exports com.sandpolis.plugin.desktop.viewer.jfx;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.plugin.desktop;
	
	provides com.sandpolis.core.instance.plugin.SandpolisPlugin with com.sandpolis.plugin.desktop.viewer.jfx.DesktopPlugin;
}
