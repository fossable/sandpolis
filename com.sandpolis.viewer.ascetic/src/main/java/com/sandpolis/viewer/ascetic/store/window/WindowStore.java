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
package com.sandpolis.viewer.ascetic.store.window;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.store.CollectionStore;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;
import com.sandpolis.viewer.ascetic.store.window.WindowStore.WindowStoreConfig;

public final class WindowStore extends CollectionStore<Window> implements ConfigurableStore<WindowStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(WindowStore.class);

	public WindowBasedTextGUI gui;

	public WindowStore() {
		super(log);
	}

	public void clear() {
		stream().forEach(gui::removeWindow);
		provider.clear();
	}

	@Override
	public void removeValue(Window value) {
		stream().filter(window -> window == value).forEach(gui::removeWindow);
		super.removeValue(value);
	}

	@Override
	public void init(Consumer<WindowStoreConfig> configurator) {
		var config = new WindowStoreConfig();
		configurator.accept(config);

		provider.initialize();
	}

	public <E extends Window> E create(Supplier<E> constructor, Consumer<E> configurator) {
		var window = add(constructor.get(), configurator);
		gui.addWindow(window);
		return window;
	}

	public final class WindowStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Window.class, Window::hashCode, null);
		}
	}

	public static final WindowStore WindowStore = new WindowStore();
}
