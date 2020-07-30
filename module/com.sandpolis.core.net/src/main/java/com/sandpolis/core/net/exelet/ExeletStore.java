package com.sandpolis.core.net.exelet;

import static com.sandpolis.core.instance.Metatypes.InstanceType.CLIENT;
import static com.sandpolis.core.instance.Metatypes.InstanceType.SERVER;
import static com.sandpolis.core.instance.Metatypes.InstanceType.VIEWER;
import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.plugin.PluginEvents.PluginLoadedEvent;
import com.sandpolis.core.instance.plugin.PluginEvents.PluginUnloadedEvent;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.instance.store.StoreMetadata;
import com.sandpolis.core.net.exelet.ExeletStore.ExeletStoreConfig;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.core.net.util.MsgUtil;

public class ExeletStore extends StoreBase<ExeletStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ExeletStore.class);

	Map<String, ExeletMethod> viewer;

	Map<String, ExeletMethod> server;

	Map<String, ExeletMethod> client;

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
				} catch (IllegalAccessException e) {
					log.error("Failed to parse Exelet", e);
					continue;
				}

				var instances = Arrays.asList(metadata.instances());
				if (instances.contains(VIEWER))
					viewer.put(exeletMethod.url, exeletMethod);
				if (instances.contains(SERVER))
					viewer.put(exeletMethod.url, exeletMethod);
				if (instances.contains(CLIENT))
					viewer.put(exeletMethod.url, exeletMethod);
			}
		}
	}

	private synchronized void unregister(Class<? extends Exelet> exelet) {
		var urlPrefix = MsgUtil.getModuleId(exelet) + "/";

		viewer.entrySet().removeIf(entry -> entry.getKey().startsWith(urlPrefix));
		server.entrySet().removeIf(entry -> entry.getKey().startsWith(urlPrefix));
		client.entrySet().removeIf(entry -> entry.getKey().startsWith(urlPrefix));
	}

	@Subscribe
	void pluginLoaded(PluginLoadedEvent event) {
		event.get().getExtensions(ExeletProvider.class).map(ExeletProvider::getExelets).flatMap(Arrays::stream)
				.forEach(this::register);
	}

	@Subscribe
	void pluginUnloaded(PluginUnloadedEvent event) {
		event.get().getExtensions(ExeletProvider.class).map(ExeletProvider::getExelets).flatMap(Arrays::stream)
				.forEach(this::unregister);
	}

	@Override
	public void init(Consumer<ExeletStoreConfig> configurator) {
		var config = new ExeletStoreConfig();
		configurator.accept(config);

		viewer = new HashMap<>();
		server = new HashMap<>();
		client = new HashMap<>();

		config.exelets.forEach(this::register);
		PluginStore.register(this);
	}

	@Override
	public StoreMetadata getMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

	public final class ExeletStoreConfig extends StoreConfig {

		/**
		 * All base {@link Exelet}s possible for the instance.
		 */
		public List<Class<? extends Exelet>> exelets;
	}

	public static final ExeletStore ExeletStore = new ExeletStore();
}
