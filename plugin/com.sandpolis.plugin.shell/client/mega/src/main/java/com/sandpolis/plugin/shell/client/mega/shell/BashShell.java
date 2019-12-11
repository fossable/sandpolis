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
package com.sandpolis.plugin.shell.client.mega.shell;

import java.util.Base64;

public class BashShell extends AbstractShell {

	@Override
	public String[] searchPath() {
		return new String[] { "/bin/sh" };
	}

	@Override
	public String[] buildSession() {
		return new String[] { location, "-i" };
	}

	@Override
	public String[] buildCommand(String command) {
		return new String[] { location, "-c",
				"echo " + Base64.getEncoder().encodeToString(command.getBytes()) + " | base64 --decode | " + location };
	}
}
