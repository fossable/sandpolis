//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common;

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
