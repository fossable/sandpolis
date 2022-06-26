//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common;

import java.util.function.Function;

import com.google.common.eventbus.Subscribe;
import org.s7s.core.instance.state.st.STAttribute;
import org.s7s.core.instance.state.st.STAttribute.ChangeEvent;

import javafx.beans.property.SimpleObjectProperty;

public class ObservableSTAttribute<T> extends SimpleObjectProperty<T> {

	private STAttribute attribute;

	private Function<Object, T> converter;

	public ObservableSTAttribute(STAttribute attribute) {
		this(attribute, null);
	}

	public ObservableSTAttribute(STAttribute attribute, Function<Object, T> converter) {
		this.attribute = attribute;
		this.converter = converter;

		set((T) attribute.get());
		attribute.addListener(this);
	}

	@Subscribe
	public void onAttributeChange(ChangeEvent event) {
		var value = event.newValue();
		if (value == null) {
			set(null);
		} else {
			set((T) value.value());
		}
	}

	@Override
	public T getValue() {
		if (converter == null) {
			return (T) attribute.get();
		} else {
			return converter.apply(attribute.get());
		}
	}

}
