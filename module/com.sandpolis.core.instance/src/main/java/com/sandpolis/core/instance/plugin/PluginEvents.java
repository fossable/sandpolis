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

import com.sandpolis.core.instance.store.event.ParameterizedEvent;

public final class PluginEvents {

	/**
	 * Indicates that a plugin was just loaded successfully.
	 */
	public static final class PluginLoadedEvent extends ParameterizedEvent<Plugin> {
		public PluginLoadedEvent(Plugin parameter) {
			super(parameter);
		}
	}

	public static final class PluginUnloadedEvent extends ParameterizedEvent<Plugin> {
		public PluginUnloadedEvent(Plugin parameter) {
			super(parameter);
		}
	}

	private PluginEvents() {
	}
}
