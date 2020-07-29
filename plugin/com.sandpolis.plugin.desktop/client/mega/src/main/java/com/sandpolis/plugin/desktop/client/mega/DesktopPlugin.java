//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.plugin.desktop.client.mega;

import com.sandpolis.core.instance.plugin.SandpolisPlugin;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.plugin.desktop.client.mega.exe.DesktopExe;

public final class DesktopPlugin extends SandpolisPlugin implements ExeletProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Exelet>[] getExelets() {
		return new Class[] { DesktopExe.class };
	}

}
