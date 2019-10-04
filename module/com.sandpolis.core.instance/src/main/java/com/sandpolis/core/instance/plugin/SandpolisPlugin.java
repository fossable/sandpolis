package com.sandpolis.core.instance.plugin;

/**
 * A class that plugins can subclass to receive lifecycle events.
 * 
 * @author cilki
 * @since 5.1.0
 */
public abstract class SandpolisPlugin {

	/**
	 * A lifecycle method that is called immediately after the plugin is loaded.
	 */
	public void loaded() {
	}

	/**
	 * A lifecycle method that is called immediately before the plugin is unloaded.
	 */
	public void unloaded() {
	}

}
