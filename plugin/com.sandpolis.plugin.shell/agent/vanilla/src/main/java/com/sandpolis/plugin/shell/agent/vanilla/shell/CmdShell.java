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
package com.sandpolis.plugin.shell.agent.vanilla.shell;

public class CmdShell extends AbstractShell {

	@Override
	public String[] searchPath() {
		return new String[] { "C:/Windows/System32/cmd.exe" };
	}

	@Override
	public String[] buildSession() {
		return new String[] { location };
	}

	@Override
	public String[] buildCommand(String command) {
		return new String[] { location, "/C", command };// TODO encode command
	}

}
