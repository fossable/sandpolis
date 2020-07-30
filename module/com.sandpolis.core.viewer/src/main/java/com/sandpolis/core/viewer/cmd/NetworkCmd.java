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
package com.sandpolis.core.viewer.cmd;

import com.sandpolis.core.net.cmdlet.Cmdlet;

/**
 * Contains network commands.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class NetworkCmd extends Cmdlet<NetworkCmd> {

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link NetworkCmd} can be invoked
	 */
	public static NetworkCmd async() {
		return new NetworkCmd();
	}

	private NetworkCmd() {
	}
}
