//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.pref;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.pref.PrefStore.PrefStoreConfig;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreBase;

/**
 * This store provides access to a unique {@link Preferences} object for
 * persistent instance settings.
 *
 * @since 5.0.0
 */
public final class PrefStore extends StoreBase implements ConfigurableStore<PrefStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(PrefStore.class);

	public PrefStore() {
		super(log);
	}

	/**
	 * The backing {@link Preferences} object.
	 */
	private Preferences container;

	/**
	 * Get the {@link Preferences} object unique to the given instance.
	 *
	 * @param instance The instance type
	 * @param flavor   The instance subtype
	 * @return A unique {@link Preferences} object
	 */
	public static Preferences getPreferences(InstanceType instance, InstanceFlavor flavor) {
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
		return container.get(tag, def);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 *
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putString(String tag, String value) {
		log.trace("Associating \"{}\": \"{}\"", tag, value);
		container.put(tag, value);
	}

	/**
	 * Get a value from the store.
	 *
	 * @param tag A unique String whose associated value is to be returned
	 * @return The boolean value associated with the provided tag
	 */
	public boolean getBoolean(String tag) {
		return container.getBoolean(tag, false);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 *
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putBoolean(String tag, boolean value) {
		log.trace("Associating \"{}\": {}", tag, value);
		container.putBoolean(tag, value);
	}

	/**
	 * Get a value from the store.
	 *
	 * @param tag A unique String whose associated value is to be returned
	 * @return The int value associated with the provided tag
	 */
	public int getInt(String tag) {
		return container.getInt(tag, 0);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 *
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putInt(String tag, int value) {
		log.trace("Associating \"{}\": {}", tag, value);
		container.putInt(tag, value);
	}

	/**
	 * Get a value from the store.
	 *
	 * @param tag A unique String whose associated value is to be returned
	 * @return The byte[] value associated with the provided tag
	 */
	public byte[] getBytes(String tag) {
		return container.getByteArray(tag, null);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 *
	 * @param tag   The unique key which will become associated with the new value
	 * @param value The new value
	 */
	public void putBytes(String tag, byte[] value) {
		log.trace("Associating \"{}\": {}", tag, value);
		container.putByteArray(tag, value);
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

		if (Arrays.stream(container.keys()).noneMatch(key -> tag.equals(key))) {
			put(tag, value);
		}
	}

	@Override
	public void close() throws BackingStoreException {
		if (container != null) {
			log.debug("Closing preference node: " + container.absolutePath());
			try {
				container.flush();
			} finally {
				container = null;
			}
		}
	}

	@Override
	public void init(Consumer<PrefStoreConfig> configurator) {
		var config = new PrefStoreConfig();
		configurator.accept(config);

		if (container != null)
			log.warn("Reinitializing store without flushing Preferences");

		if (config.prefNodeClass != null) {
			container = Preferences.userNodeForPackage(config.prefNodeClass);
		} else if (config.instance != null && config.flavor != null) {
			container = getPreferences(config.instance, config.flavor);
		}

		try {
			for (var entry : config.defaults.entrySet())
				register(entry.getKey(), entry.getValue());
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
	}

	@ConfigStruct
	public static final class PrefStoreConfig {
		public InstanceType instance;
		public InstanceFlavor flavor;
		public Class<?> prefNodeClass;

		public final Map<String, Object> defaults = new HashMap<>();
	}

	public static final PrefStore PrefStore = new PrefStore();
}
