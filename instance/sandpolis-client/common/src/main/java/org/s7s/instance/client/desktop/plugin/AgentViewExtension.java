//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.plugin;

import org.s7s.core.instance.state.st.STDocument;

import tornadofx.View;

public abstract class AgentViewExtension extends View {

	private String name;

	public AgentViewExtension(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract void nowVisible(STDocument profile);

	public abstract void nowInvisible();
}
