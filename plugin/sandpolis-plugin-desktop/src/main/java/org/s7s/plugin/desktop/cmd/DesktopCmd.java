//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.desktop.cmd;

import java.util.concurrent.CompletionStage;

import org.s7s.core.instance.cmdlet.Cmdlet;
import org.s7s.plugin.desktop.Messages.RQ_CaptureScreenshot;
import org.s7s.plugin.desktop.Messages.RS_CaptureScreenshot;

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
	public CompletionStage<RS_CaptureScreenshot> screenshot() {
		return request(RS_CaptureScreenshot.class, RQ_CaptureScreenshot.newBuilder());
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
