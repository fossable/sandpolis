//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.stage;

import static org.s7s.core.instance.pref.PrefStore.PrefStore;

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.store.CollectionStore;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * The {@link StageStore} keeps track of the application's loaded
 * {@link Stage}s.
 *
 * @since 5.0.0
 */
public final class StageStore extends CollectionStore<SandpolisStage> {

	private static final Logger log = LoggerFactory.getLogger(StageStore.class);

	public StageStore() {
		super(log);
	}

	/**
	 * Hide all stages in the store.
	 */
	public void hideAll() {
		Platform.runLater(() -> {
			stream().forEach(stage -> stage.hide());
		});
	}

	/**
	 * Show all stages in the store.
	 */
	public void showAll() {
		Platform.runLater(() -> {
			stream().forEach(stage -> stage.show());
		});
	}

	/**
	 * Close a stage.
	 *
	 * @param stage The stage to close
	 */
	public void close(SandpolisStage stage) {
		removeValue(stage);
		Platform.runLater(() -> {
			stage.close();
		});
	}

	/**
	 * Change the application's global theme.
	 *
	 * @param theme The new theme
	 */
	public void changeTheme(String theme) {
		Objects.requireNonNull(theme);

		PrefStore.putString("ui.theme", theme);
		Platform.runLater(() -> {
			stream().map(stage -> stage.getScene().getStylesheets()).forEach(styles -> {
				styles.clear();
				styles.add("/css/" + theme + ".css");
			});
		});
	}

	public SandpolisStage create(Consumer<SandpolisStage> configurator) {
		var stage = new SandpolisStage();
		configurator.accept(stage);

		Platform.runLater(() -> {
			configurator.accept(stage);

			stage.show();
		});

		return stage;
	}

	public static final StageStore StageStore = new StageStore();
}
