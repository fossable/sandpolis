//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
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
