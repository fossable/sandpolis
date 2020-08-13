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
package com.sandpolis.viewer.lifegem.stage;

import static com.sandpolis.core.instance.pref.PrefStore.PrefStore;

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.viewer.lifegem.stage.StageStore.StageStoreConfig;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * The {@link StageStore} keeps track of the application's loaded
 * {@link Stage}s.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class StageStore extends CollectionStore<SandpolisStage> implements ConfigurableStore<StageStoreConfig> {

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

	@Override
	public void init(Consumer<StageStoreConfig> configurator) {
		var config = new StageStoreConfig();
		configurator.accept(config);

		provider.initialize();
	}

	public SandpolisStage create(Consumer<SandpolisStage> configurator) {
		var stage = add(new SandpolisStage(), configurator);

		Platform.runLater(() -> {
			configurator.accept(stage);

			stage.show();
		});

		return stage;
	}

	public final class StageStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(SandpolisStage.class, SandpolisStage::hashCode, null);
		}
	}

	public static final StageStore StageStore = new StageStore();
}
