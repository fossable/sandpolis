//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.state.STStore.STStoreConfig;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.STAttribute;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.StoreBase;

public final class STStore extends StoreBase implements ConfigurableStore<STStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(STStore.class);

	/**
	 * The root of the instance's state tree.
	 */
	private STDocument root;

	private ExecutorService service;

	public STStore() {
		super(log);
	}

	public STDocument root() {
		return root;
	}

	public ExecutorService pool() {
		return service;
	}

	@Override
	public void init(Consumer<STStoreConfig> configurator) {
		var config = new STStoreConfig(configurator);

		service = Executors.newFixedThreadPool(config.concurrency);
		root = config.root;
	}

	@Override
	public void close() throws Exception {
		service.shutdown();
	}

	public final class STStoreConfig {
		public int concurrency = 1;
		public STDocument root;

		private STStoreConfig(Consumer<STStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final STStore STStore = new STStore();

	public STDocument get(Oid oid) {
		return root.document(oid);
	}

	public STAttribute attribute(Oid oid) {
		return root.attribute(oid);
	}

}
