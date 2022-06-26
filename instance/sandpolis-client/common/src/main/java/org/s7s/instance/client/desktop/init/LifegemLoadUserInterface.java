//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.init;

import static org.s7s.instance.client.desktop.stage.StageStore.StageStore;

import org.s7s.instance.client.desktop.ui.common.FxUtil;
import org.s7s.core.instance.InitTask;

import javafx.application.Application;
import javafx.stage.Stage;
import tornadofx.FX;

public class LifegemLoadUserInterface extends InitTask {

	/**
	 * The {@link Application} class for starting the user interface.
	 */
	public static class UI extends Application {

		private static Application singleton;

		public UI() {
			if (singleton != null)
				throw new IllegalStateException();

			singleton = this;
		}

		/**
		 * Get the application handle.
		 *
		 * @return The {@link Application} or {@code null} if it has not started
		 */
		public static Application getApplication() {
			return singleton;
		}

		@Override
		public void start(Stage main) throws Exception {

			// Ignore the primary stage and create a new one
			var stage = StageStore.create(s -> {
				s.setRoot("org.s7s.instance.client.desktop.ui.login.LoginView");
				// s.setWidth(PrefStore.getInt("ui.view.login.width"));
				// s.setHeight(PrefStore.getInt("ui.view.login.height"));
				// s.setResizable(false);
				s.setTitle(FxUtil.translate("stage.login.title"));
			});

			// Register application
			FX.registerApplication(this, stage);

		}
	}

	@Override
	public TaskOutcome run(TaskOutcome.Factory outcome) throws Exception {
		new Thread(() -> Application.launch(UI.class)).start();

		return outcome.succeeded();
	}

	@Override
	public String description() {
		return "Load user interface";
	}

}
