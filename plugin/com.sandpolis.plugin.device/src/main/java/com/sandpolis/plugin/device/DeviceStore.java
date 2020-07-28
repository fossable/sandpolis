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
package com.sandpolis.plugin.device;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.provider.MemoryMapStoreProvider;

public class DeviceStore extends MapStore<Device, StoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(DeviceStore.class);

	public DeviceStore() {
		super(log);
	}

	@Override
	public void init(Consumer<StoreConfig> configurator) {
		var config = new DeviceStoreConfig();
		configurator.accept(config);

		provider.initialize();
	}

	public final class DeviceStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Device.class, Device::tag);
		}
	}
}
