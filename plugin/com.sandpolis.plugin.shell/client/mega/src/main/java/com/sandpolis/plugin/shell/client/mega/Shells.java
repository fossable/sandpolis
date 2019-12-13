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
package com.sandpolis.plugin.shell.client.mega;

import com.sandpolis.plugin.shell.client.mega.shell.BashShell;
import com.sandpolis.plugin.shell.client.mega.shell.CmdShell;
import com.sandpolis.plugin.shell.client.mega.shell.PwshShell;
import com.sandpolis.plugin.shell.client.mega.shell.ZshShell;

public final class Shells {

	public static final PwshShell PWSH = new PwshShell();

	public static final CmdShell CMD = new CmdShell();

	public static final BashShell BASH = new BashShell();

	public static final ZshShell ZSH = new ZshShell();

	private Shells() {
	}
}
