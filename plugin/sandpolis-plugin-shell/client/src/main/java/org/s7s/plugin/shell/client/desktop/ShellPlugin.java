//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.shell.client.lifegem;

import org.s7s.instance.client.desktop.plugin.AgentViewExtension;
import org.s7s.instance.client.desktop.plugin.AgentViewProvider;
import org.s7s.core.instance.plugin.SandpolisPlugin;
//import org.s7s.plugin.shell.client.lifegem.ShellView;

public class ShellPlugin extends SandpolisPlugin implements AgentViewProvider {

	@Override
	public AgentViewExtension[] getAgentViews() {
		// return new AgentViewExtension[] { new ShellView() };
		return null;
	}

}
