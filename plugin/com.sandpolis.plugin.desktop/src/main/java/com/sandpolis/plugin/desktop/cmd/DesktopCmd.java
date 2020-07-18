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
package com.sandpolis.plugin.desktop.cmd;

import java.util.concurrent.CompletionStage;

import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.plugin.desktop.msg.MsgDesktop.RQ_Screenshot;
import com.sandpolis.plugin.desktop.msg.MsgDesktop.RS_Screenshot;

/**
 * Contains desktop commands.
 *
 * @author cilki
 * @since 5.0.2
 */
public final class DesktopCmd extends Cmdlet<DesktopCmd> {

	/**
	 * Take a desktop screenshot.
	 *
	 * @return A response future
	 */
	public CompletionStage<RS_Screenshot> screenshot() {
		return request(RS_Screenshot.class, RQ_Screenshot.newBuilder());
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link DesktopCmd} can be invoked
	 */
	public static DesktopCmd async() {
		return new DesktopCmd();
	}

	private DesktopCmd() {
	}
}
