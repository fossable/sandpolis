//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common.controller;

import java.util.Objects;

import com.google.common.eventbus.Subscribe;
import org.s7s.instance.client.desktop.stage.SandpolisStage;

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
