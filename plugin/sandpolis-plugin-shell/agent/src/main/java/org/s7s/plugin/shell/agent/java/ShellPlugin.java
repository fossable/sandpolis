//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.shell.agent.java;

import org.s7s.core.instance.plugin.SandpolisPlugin;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.plugin.ExeletProvider;

public final class ShellPlugin extends SandpolisPlugin implements ExeletProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Exelet>[] getExelets() {
		return new Class[] { ShellExe.class };
	}
}
