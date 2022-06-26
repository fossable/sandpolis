//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.exelet;

import static org.s7s.core.foundation.Instance.InstanceType.AGENT;
import static org.s7s.core.foundation.Instance.InstanceType.CLIENT;
import static org.s7s.core.foundation.Instance.InstanceType.SERVER;
import static org.s7s.core.instance.plugin.PluginStore.PluginStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import org.s7s.core.instance.plugin.PluginStore.PluginLoadedEvent;
import org.s7s.core.instance.plugin.PluginStore.PluginUnloadedEvent;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.StoreBase;
import org.s7s.core.instance.exelet.ExeletStore.ExeletStoreConfig;
import org.s7s.core.instance.plugin.ExeletProvider;
import org.s7s.core.instance.util.S7SMsg;

public class ExeletStore extends StoreBase implements ConfigurableStore<ExeletStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ExeletStore.class);

	Map<Integer, ExeletMethod> client;

	Map<Integer, ExeletMethod> server;

	Map<Integer, ExeletMethod> agent;

	public ExeletStore() {
		super(log);
	}

	private synchronized void register(Class<? extends Exelet> exelet) {
		for (var method : exelet.getMethods()) {
			var metadata = method.getAnnotation(Exelet.Handler.class);
			if (metadata != null) {
				ExeletMethod exeletMethod;
				try {
					exeletMethod = new ExeletMethod(method);
				} catch (Exception e) {
					log.error("Failed to parse Exelet", e);
					continue;
				}

				log.trace("Registering exelet handler: {} ({})", exeletMethod.name, exeletMethod.type);

				var instances = Arrays.asList(metadata.instances());
				if (instances.contains(CLIENT))
					client.put(exeletMethod.type, exeletMethod);
				if (instances.contains(SERVER))
					server.put(exeletMethod.type, exeletMethod);
				if (instances.contains(AGENT))
					agent.put(exeletMethod.type, exeletMethod);
			}
		}
	}

	private synchronized void unregister(Class<? extends Exelet> exelet) {
		for (var method : exelet.getMethods()) {
			int removal = S7SMsg.getPayloadType(method);

			client.remove(removal);
			server.remove(removal);
			agent.remove(removal);
		}
	}

	@Subscribe
	void pluginLoaded(PluginLoadedEvent event) {
		event.plugin().getHandle(ExeletProvider.class).ifPresent(handle -> {
			for (var exelet : handle.getExelets()) {
				register(exelet);
			}
		});
	}

	@Subscribe
	void pluginUnloaded(PluginUnloadedEvent event) {
		event.plugin().getHandle(ExeletProvider.class).ifPresent(handle -> {
			for (var exelet : handle.getExelets()) {
				unregister(exelet);
			}
		});
	}

	@Override
	public void init(Consumer<ExeletStoreConfig> configurator) {
		var config = new ExeletStoreConfig(configurator);

		client = new HashMap<>();
		server = new HashMap<>();
		agent = new HashMap<>();

		config.exelets.forEach(this::register);
		PluginStore.register(this);
	}

	public static final class ExeletStoreConfig {

		/**
		 * All base {@link Exelet}s possible for the instance.
		 */
		public final List<Class<? extends Exelet>> exelets = new ArrayList<>();

		private ExeletStoreConfig(Consumer<ExeletStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final ExeletStore ExeletStore = new ExeletStore();
}
