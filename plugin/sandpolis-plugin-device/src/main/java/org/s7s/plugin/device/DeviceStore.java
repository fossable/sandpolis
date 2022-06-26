//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.device;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;
import org.s7s.plugin.device.DeviceStore.DeviceStoreConfig;

public class DeviceStore extends STCollectionStore<Device> implements ConfigurableStore<DeviceStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(DeviceStore.class);

	public DeviceStore() {
		super(log, Device::new);
	}

	@Override
	public void init(Consumer<DeviceStoreConfig> configurator) {
		var config = new DeviceStoreConfig(configurator);

		setDocument(config.collection);
	}

	public static final class DeviceStoreConfig {

		public STDocument collection;

		private DeviceStoreConfig(Consumer<DeviceStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final DeviceStore DeviceStore = new DeviceStore();
}
