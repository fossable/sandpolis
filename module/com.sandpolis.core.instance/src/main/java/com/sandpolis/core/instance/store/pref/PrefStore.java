/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.instance.store.pref;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.instance.store.pref.PrefStore.PrefStoreConfig;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

/**
 * This store provides access to a unique {@link Preferences} object for
 * persistent instance settings.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class PrefStore extends StoreBase<PrefStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(PrefStore.class);

	/**
	 * The backing {@link Preferences} object.
	 */
	private Preferences provider;

	/**
	 * Get the {@link Preferences} object unique to the given instance.
	 * 
	 * @param instance The instance type
	 * @param flavor   The instance subtype
	 * @return A unique {@link Preferences} object
	 */
	public static Preferences getPreferences(Instance instance, InstanceFlavor flavor) {
		return Preferences.userRoot()
				.node("com/sandpolis/" + instance.name().toLowerCase() + "/" + flavor.name().toLowerCase());
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The String value associated with the provided tag
	 */
	public String getString(String tag) {
		return getString(tag, null);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @param def The default value
	 * @return The String value associated with the provided tag
	 */
	public String getString(String tag, String def) {
		return provider.get(tag, def);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putString(String tag, String value) {
		log.trace("Associating \"{}\": \"{}\"", tag, value);
		provider.put(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The boolean value associated with the provided tag
	 */
	public boolean getBoolean(String tag) {
		return provider.getBoolean(tag, false);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putBoolean(String tag, boolean value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putBoolean(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The int value associated with the provided tag
	 */
	public int getInt(String tag) {
		return provider.getInt(tag, 0);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putInt(String tag, int value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putInt(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag A unique String whose associated value is to be returned
	 * @return The byte[] value associated with the provided tag
	 */
	public byte[] getBytes(String tag) {
		return provider.getByteArray(tag, null);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putBytes(String tag, byte[] value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putByteArray(tag, value);
	}

	/**
	 * Add a general value to the store. Old values are overwritten.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void put(String tag, Object value) {
		Objects.requireNonNull(tag);
		Objects.requireNonNull(value);

		if (value instanceof String)
			putString(tag, (String) value);
		else if (value instanceof Boolean)
			putBoolean(tag, (Boolean) value);
		else if (value instanceof Integer)
			putInt(tag, (Integer) value);
		else if (value instanceof byte[])
			putBytes(tag, (byte[]) value);
		else
			throw new IllegalArgumentException("Cannot store value of type: " + value.getClass().getName());
	}

	/**
	 * Add a value to the store unless it's already present.
	 * 
	 * @param tag   The unique key which will become associated with the new value
	 *              unless it's already associated with a value
	 * @param value The new value
	 * @throws BackingStoreException
	 */
	public void register(String tag, Object value) throws BackingStoreException {
		Objects.requireNonNull(tag);
		Objects.requireNonNull(value);

		if (Arrays.stream(provider.keys()).noneMatch(key -> tag.equals(key))) {
			put(tag, value);
		}
	}

	@Override
	public void close() throws BackingStoreException {
		log.debug("Closing PrefStore (provider: " + provider + ")");
		try {
			provider.flush();
		} finally {
			provider = null;
		}
	}

	@Override
	public PrefStore init(Consumer<PrefStoreConfig> configurator) {
		var config = new PrefStoreConfig();
		configurator.accept(config);

		if (provider != null)
			log.warn("Reinitializing store without flushing Preferences");

		if (config.prefNodeClass != null) {
			provider = Preferences.userNodeForPackage(config.prefNodeClass);
		} else if (config.instance != null && config.flavor != null) {
			provider = getPreferences(config.instance, config.flavor);
		}

		try {
			for (var entry : config.defaults.entrySet())
				register(entry.getKey(), entry.getValue());
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}

		return (PrefStore) super.init(null);
	}

	public final class PrefStoreConfig extends StoreConfig {
		public Instance instance;
		public InstanceFlavor flavor;
		public Class<?> prefNodeClass;

		public final Map<String, Object> defaults = new HashMap<>();
	}

	public static final PrefStore PrefStore = new PrefStore();
}
