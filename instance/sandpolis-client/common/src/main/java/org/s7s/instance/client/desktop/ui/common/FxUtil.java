//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common;

import static org.s7s.core.instance.state.STStore.STStore;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.STAttribute;
import org.s7s.core.instance.state.st.STDocument;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Miscellaneous JavaFX utilities.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class FxUtil {

	private static final Logger log = LoggerFactory.getLogger(FxUtil.class);

	/**
	 * The application's global resource bundle.
	 */
	private static final ResourceBundle bundle;

	static {
		ResourceBundle _bundle;
		try {
			_bundle = ResourceBundle.getBundle("text.application");
		} catch (MissingResourceException e) {
			log.warn(e.getMessage());

			// Empty bundle
			_bundle = new ResourceBundle() {

				@Override
				protected Object handleGetObject(String key) {
					return null;
				}

				@Override
				public Enumeration<String> getKeys() {
					return null;
				}
			};
		}
		bundle = _bundle;
	}

	/**
	 * Get the resource bundle for the current locale.
	 *
	 * @return The global resource bundle
	 */
	public static ResourceBundle getResources() {
		return bundle;
	}

	/**
	 * A convenience method for {@code getResources().getString()}.
	 *
	 * @param key The translation key
	 * @return The string associated with the given key or {@code null}
	 */
	public static String translate(String key) {
		return bundle.getString(key);
	}

	public static <T> ObservableValue<T> newProperty(STAttribute attribute) {
		return new ObservableSTAttribute<>(attribute);
	}

	public static <T> ObservableValue<T> newProperty(STAttribute attribute, Function<Object, T> converter) {
		return new ObservableSTAttribute<>(attribute, converter);
	}

	public static ObservableList<STDocument> newObservable(Oid oid) {
		return new ObservableSTDocument(STStore.get(oid));
	}

	public static FilteredList<STDocument> newObservable(Oid oid, Predicate<STDocument> filter) {
		return new FilteredList<>(newObservable(oid), filter);
	}

	private FxUtil() {
	}

}
