//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common.pane;

import static org.s7s.core.instance.pref.PrefStore.PrefStore;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.testfx.assertions.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@Disabled
@ExtendWith(ApplicationExtension.class)
class CarouselPaneTest {

	@BeforeAll
	private static void init() throws Exception {
		PrefStore.init(config -> {
			config.prefNodeClass = CarouselPaneTest.class;

			config.defaults.put("ui.animations", true);
		});

		System.setProperty("java.awt.headless", "true");
		System.setProperty("testfx.headless", "true");
		System.setProperty("testfx.robot", "glass");
		System.setProperty("prism.order", "sw");
	}

	private Label view1;
	private Label view2;
	private Label view3;
	private CarouselPane carousel;

	@Start
	private void start(Stage stage) {
		view1 = new Label("1");
		view1.setId("view1");
		view2 = new Label("2");
		view2.setId("view2");
		view3 = new Label("3");
		view3.setId("view3");

		carousel = new CarouselPane(view1);
		carousel.add(view2);
		carousel.add(view3);

		stage.setScene(new Scene(carousel, 100, 100));
		stage.show();
	}

	@Test
	@DisplayName("Test a multi-view carousel")
	void carouselPane_3(FxRobot robot) throws Exception {
		assertFalse(carousel.isMoving());
		assertThat(carousel).hasChild("#view1");
		assertThat(carousel).doesNotHaveChild("#view2");
		assertThat(carousel).doesNotHaveChild("#view3");

		// Move view and check state
		robot.interact(() -> carousel.moveForward());
		await().atMost(5000, MILLISECONDS).until(() -> !carousel.isMoving());
		Thread.sleep(400); // A little extra time
		assertThat(carousel).doesNotHaveChild("#view1");
		assertThat(carousel).hasChild("#view2");
		assertThat(carousel).doesNotHaveChild("#view3");

		// Move view and check state
		robot.interact(() -> carousel.moveForward());
		await().atMost(5000, MILLISECONDS).until(() -> !carousel.isMoving());
		Thread.sleep(400); // A little extra time
		assertThat(carousel).doesNotHaveChild("#view1");
		assertThat(carousel).doesNotHaveChild("#view2");
		assertThat(carousel).hasChild("#view3");

		// Move view and check state
		robot.interact(() -> carousel.moveBackward());
		await().atMost(5000, MILLISECONDS).until(() -> !carousel.isMoving());
		Thread.sleep(400); // A little extra time
		assertThat(carousel).doesNotHaveChild("#view1");
		assertThat(carousel).hasChild("#view2");
		assertThat(carousel).doesNotHaveChild("#view3");

		// Move view and check state
		robot.interact(() -> carousel.moveBackward());
		await().atMost(5000, MILLISECONDS).until(() -> !carousel.isMoving());
		Thread.sleep(400); // A little extra time
		assertThat(carousel).hasChild("#view1");
		assertThat(carousel).doesNotHaveChild("#view2");
		assertThat(carousel).doesNotHaveChild("#view3");
	}
}
