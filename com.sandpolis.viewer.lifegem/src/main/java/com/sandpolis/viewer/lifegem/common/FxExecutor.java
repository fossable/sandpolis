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
package com.sandpolis.viewer.lifegem.common;

import java.util.concurrent.Executor;

import javafx.application.Platform;

public final class FxExecutor implements Executor {

	public static final FxExecutor INSTANCE = new FxExecutor();

	private FxExecutor() {
	}

	@Override
	public void execute(Runnable command) {
		Platform.runLater(command);
	}
}
