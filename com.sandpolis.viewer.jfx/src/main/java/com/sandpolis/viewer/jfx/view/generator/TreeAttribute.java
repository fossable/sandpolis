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
package com.sandpolis.viewer.jfx.view.generator;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/**
 * Represents an attribute cell in the configuration tree.
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class TreeAttribute extends GenTreeItem {

	/**
	 * The attribute's value.
	 */
	private StringProperty value = new SimpleStringProperty();

	/**
	 * The validity of the attribute's value.
	 */
	private BooleanProperty validity = new SimpleBooleanProperty();

	/**
	 * The function that determines the validity of the value.
	 */
	protected Function<String, Boolean> validator = s -> true;

	/**
	 * The attribute's custom control.
	 */
	protected BorderPane control = new BorderPane();

	public TreeAttribute(String name) {
		super(Type.ATTRIBUTE, name);

		Label label = new Label(name().get());
		label.graphicProperty().bind(icon());
		label.textFillProperty().bind(Bindings.createObjectBinding(() -> {
			return validity.get() ? Color.BLACK : Color.RED;
		}, validity));
		control.setLeft(label);

		validity.bind(Bindings.createBooleanBinding(() -> {
			return validator.apply(value.get());
		}, value));

	}

	/**
	 * Get the attribute's custom control.
	 * 
	 * @return The control
	 */
	public Node getControl() {
		return control;
	}

	/**
	 * Get the {@code value} property.
	 * 
	 * @return The {@code value} property
	 */
	public StringProperty value() {
		return value;
	}

	/**
	 * Get the {@code validity} property.
	 * 
	 * @return The {@code validity} property
	 */
	public BooleanProperty validity() {
		return validity;
	}

}