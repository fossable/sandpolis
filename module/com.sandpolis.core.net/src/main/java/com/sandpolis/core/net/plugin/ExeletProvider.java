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
package com.sandpolis.core.net.plugin;

import com.google.protobuf.Message;
import com.sandpolis.core.net.command.Exelet;

/**
 * @author cilki
 * @since 5.0.0
 */
public interface ExeletProvider {

	/**
	 * Get the Exelet classes that the plugin contains.
	 *
	 * @return A list of Exelet classes
	 */
	public Class<? extends Exelet>[] getExelets();

	public Class<? extends Message> getMessageType();
}
