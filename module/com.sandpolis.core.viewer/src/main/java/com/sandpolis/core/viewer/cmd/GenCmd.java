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

import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.core.net.MsgGenerator.RQ_Generate;
import com.sandpolis.core.net.MsgGenerator.RS_Generate;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;

/**
 * Contains generator commands.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GenCmd extends Cmdlet<GenCmd> {

	/**
	 * Generate a payload from the given configuration.
	 *
	 * @param config The generation configuration
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<RS_Generate> generate(GenConfig config) {
		return request(RQ_Generate.newBuilder().setConfig(config));
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link GenCmd} can be invoked
	 */
	public static GenCmd async() {
		return new GenCmd();
	}

	private GenCmd() {
	}
}
