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

	private List<ChangeListener<? super E>> listeners = new LinkedList<>();

	@Override
	public void addListener(InvalidationListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addListener(ChangeListener<? super E> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ChangeListener<? super E> listener) {
		listeners.remove(listener);
	}

	@Override
	public void set(E value) {
		listeners.forEach(listener -> listener.changed(this, get(), value));
		super.set(value);
	}

	@Override
	public void set(E value, long time) {
		listeners.forEach(listener -> listener.changed(this, get(), value));
		super.set(value, time);
	}

	@Override
	public E getValue() {
		return get();
	}

}
