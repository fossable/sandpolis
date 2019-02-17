/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.attribute;

import java.util.LinkedList;
import java.util.List;

import com.sandpolis.core.attribute.Attribute;
import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.attribute.UntrackedAttribute;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * This {@link Attribute} is intended to be bound to a JavaFX {@link Property}
 * for extremely easy updates.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ObservableAttribute<E> extends UntrackedAttribute<E> implements ObservableValue<E> {

	/**
	 * The list of change listeners for this {@link ObservableAttribute}.
	 */
	private List<ChangeListener<? super E>> changeListeners = new LinkedList<>();

	/**
	 * The list of invalidation listeners for this {@link ObservableAttribute}.
	 */
	private List<InvalidationListener> invalidationListeners = new LinkedList<>();

	public ObservableAttribute(AttributeKey<E> key) {
		super(key);
	}

	@Override
	public void addListener(InvalidationListener listener) {
		invalidationListeners.add(listener);
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		invalidationListeners.remove(listener);
	}

	@Override
	public void addListener(ChangeListener<? super E> listener) {
		changeListeners.add(listener);
	}

	@Override
	public void removeListener(ChangeListener<? super E> listener) {
		changeListeners.remove(listener);
	}

	@Override
	public void set(E value) {
		invalidationListeners.forEach(listener -> listener.invalidated(this));
		changeListeners.forEach(listener -> listener.changed(this, get(), value));
		super.set(value);
	}

	@Override
	public void set(E value, long time) {
		invalidationListeners.forEach(listener -> listener.invalidated(this));
		changeListeners.forEach(listener -> listener.changed(this, get(), value));
		super.set(value, time);
	}

	@Override
	public E getValue() {
		return get();
	}

}
