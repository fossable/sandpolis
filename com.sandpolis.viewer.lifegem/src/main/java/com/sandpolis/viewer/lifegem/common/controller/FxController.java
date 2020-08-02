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
package com.sandpolis.viewer.lifegem.common.controller;

import java.util.Objects;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.viewer.lifegem.stage.SandpolisStage;

/**
 * A controller that contains convenience fields.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class FxController extends AbstractController {

	protected SandpolisStage stage;

	@Subscribe
	public void setStage(SandpolisStage stage) {
		if (this.stage != null)
			throw new IllegalStateException();

		this.stage = Objects.requireNonNull(stage);
	}

}
