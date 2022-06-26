//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.init;

import static org.s7s.core.instance.plugin.PluginStore.PluginStore;

import org.s7s.core.instance.InitTask;
import org.s7s.core.instance.InstanceContext;

public class InstanceLoadPlugins extends InitTask {

	@Override
	public boolean enabled() {
		return InstanceContext.PLUGIN_ENABLED.get();
	}

	@Override
	public TaskOutcome run(TaskOutcome.Factory outcome) throws Exception {
		PluginStore.scanPluginDirectory();
		PluginStore.loadPlugins();

		return outcome.succeeded();
	}

	@Override
	public String description() {
		return "Load plugins";
	}

}
