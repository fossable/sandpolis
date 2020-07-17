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

import java.util.concurrent.CompletionStage;

import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.sv.msg.MsgGenerator.RQ_Generate;
import com.sandpolis.core.sv.msg.MsgGenerator.RS_Generate;

/**
 * An API for interacting with generators on the server.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class GenCmd extends Cmdlet<GenCmd> {

	/**
	 * Generate a payload from the given configuration.
	 *
	 * @param config The generation configuration
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<RS_Generate> generate(GenConfig config) {
		return request(RS_Generate.class, RQ_Generate.newBuilder().setConfig(config));
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
