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

import java.util.Objects;
import java.util.function.Function;

import javafx.scene.control.TextField;

/**
 * Represents a text attribute cell in the configuration tree.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class TreeAttributeText extends TreeAttribute {

	private TextField text = new TextField();

	public TreeAttributeText(String name) {
		super(name);

		// Setup value binding
		value().bind(text.textProperty());

		// Add control
		control.setRight(text);
	}

	public TreeAttributeText validator(Function<String, Boolean> validator) {
		this.validator = Objects.requireNonNull(validator);
		return this;
	}

	/**
	 * Set the current value.
	 * 
	 * @param value The new value
	 * @return {@code this}
	 */
	public TreeAttributeText value(String value) {
		this.text.setText(value);
		return this;
	}

	/**
	 * Set the number of character columns.
	 * 
	 * @param number The new value
	 * @return {@code this}
	 */
	public TreeAttributeText columns(int number) {
		this.text.setPrefColumnCount(number);
		return this;
	}

}