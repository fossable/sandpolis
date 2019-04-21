/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.view.generator.config_tree;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;

/**
 * A controller for items that have an attribute.
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class TreeAttributeController extends TreeItemController {

	/**
	 * The attribute's ID.
	 */
	private StringProperty id = new SimpleStringProperty();

	/**
	 * Get the {@code id} property.
	 * 
	 * @return The {@code id} property
	 */
	public StringProperty id() {
		return id;
	}

	/**
	 * The validity of the attribute's value.
	 */
	private BooleanProperty validity = new SimpleBooleanProperty();

	/**
	 * Get the {@code validity} property.
	 * 
	 * @return The {@code validity} property
	 */
	public BooleanProperty validity() {
		return validity;
	}

	/**
	 * The function that determines the validity of the attribute value.
	 */
	protected ObjectProperty<Function<String, Boolean>> validator = new SimpleObjectProperty<>(s -> true);

	/**
	 * Get the {@code validator} property.
	 * 
	 * @return The {@code validator} property
	 */
	public ObjectProperty<Function<String, Boolean>> validator() {
		return validator;
	}

	@FXML
	protected void initialize() {
		name.textFillProperty().bind(Bindings.createObjectBinding(() -> {
			return validity.get() ? Color.BLACK : Color.RED;
		}, validity));

		validity.bind(Bindings.createBooleanBinding(() -> {
			return validator.get().apply(value().get());
		}, value()));

	}

	/**
	 * Get the {@code value} property.
	 * 
	 * @return The {@code value} property
	 */
	public abstract StringProperty value();

}