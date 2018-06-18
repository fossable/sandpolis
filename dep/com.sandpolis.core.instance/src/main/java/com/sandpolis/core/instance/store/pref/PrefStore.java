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

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Store;

/**
 * This store provides static access to a unique Preferences object for the
 * instance. This store should be used for settings that should not exist in the
 * instance database.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class PrefStore extends Store {
	private PrefStore() {
	}

	private static final Logger log = LoggerFactory.getLogger(PrefStore.class);

	/**
	 * The backing {@code Preferences} object.
	 */
	private static Preferences provider;

	/**
	 * Load the store from the given {@link Preferences}.
	 * 
	 * @param prefs
	 *            The store provider
	 */
	public static void load(Preferences prefs) {
		if (prefs == null)
			throw new IllegalArgumentException();
		if (provider != null)
			throw new IllegalStateException();

		provider = prefs;
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag
	 *            A unique String whose associated value is to be returned.
	 * @return The String value associated with the provided tag.
	 */
	public static String getString(String tag) {
		return provider.get(tag, null);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag
	 *            The unique key which will become associated with the new value.
	 * @param value
	 *            The new value
	 */
	public static void putString(String tag, String value) {
		log.trace("Associating \"{}\": \"{}\"", tag, value);
		provider.put(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag
	 *            A unique String whose associated value is to be returned.
	 * @return The boolean value associated with the provided tag.
	 */
	public static boolean getBoolean(String tag) {
		return provider.getBoolean(tag, false);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag
	 *            The unique key which will become associated with the new value.
	 * @param value
	 *            The new value
	 */
	public static void putBoolean(String tag, boolean value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putBoolean(tag, value);
	}

	/**
	 * Get a value from the store.
	 * 
	 * @param tag
	 *            A unique String whose associated value is to be returned.
	 * @return The int value associated with the provided tag.
	 */
	public static int getInt(String tag) {
		return provider.getInt(tag, 0);
	}

	/**
	 * Add a value to the store. Old values are overwritten.
	 * 
	 * @param tag
	 *            The unique key which will become associated with the new value.
	 * @param value
	 *            The new value
	 */
	public static void putInt(String tag, int value) {
		log.trace("Associating \"{}\": {}", tag, value);
		provider.putInt(tag, value);
	}

	/**
	 * Flush and shutdown the store. Any subsequent interaction will fail until the
	 * store is reinitialized.
	 */
	public static void close() throws Exception {
		try {
			provider.flush();
		} finally {
			provider = null;
		}
	}

}
