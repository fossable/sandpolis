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

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.state.vst.VirtCollection;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.STCollectionStore;
import com.sandpolis.plugin.device.DeviceStore.DeviceStoreConfig;
import com.sandpolis.plugin.device.state.VirtDevice;

public class DeviceStore extends STCollectionStore<Device> implements ConfigurableStore<DeviceStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(DeviceStore.class);

	public DeviceStore() {
		super(log);
	}

	@Override
	public void init(Consumer<DeviceStoreConfig> configurator) {
		var config = new DeviceStoreConfig();
		configurator.accept(config);

		collection = config.collection;
	}

	public Device create(Consumer<VirtDevice> configurator) {
		return add(configurator, Device::new);
	}

	@ConfigStruct
	public static final class DeviceStoreConfig {

		public VirtCollection<VirtDevice> collection;
	}

	public static final DeviceStore DeviceStore = new DeviceStore();
}
