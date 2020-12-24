//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.plugin.filesys.agent.vanilla;

import com.sandpolis.core.instance.plugin.SandpolisPlugin;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.plugin.filesys.agent.vanilla.exe.FilesysExe;

public final class FilesysPlugin extends SandpolisPlugin implements ExeletProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Exelet>[] getExelets() {
		return new Class[] { FilesysExe.class };
	}
}
