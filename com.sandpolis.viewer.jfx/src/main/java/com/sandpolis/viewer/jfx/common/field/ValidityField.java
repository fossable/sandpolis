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
package com.sandpolis.viewer.jfx.common.field;

import java.util.Objects;
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/**
 * A {@link TextField} that keeps track of its current validity and changes its
 * appearance accordingly.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ValidityField extends TextField {

	/**
	 * A function that determines the field's validity.
	 */
	private Function<String, Boolean> validator;

	private final BooleanProperty validity;

	public ValidityField() {
		this("");
	}

	public ValidityField(String initial) {
		super(initial);

		validity = new SimpleBooleanProperty(true);

		// Set default validator
		setValidator(s -> true);

		// Setup listener
		textProperty().addListener((p, o, n) -> {
			validity.set(validator.apply(n));
		});

		// Visual errors
		validity.addListener((p, o, n) -> {
			if (n) {
				setBorder(new Border(new BorderStroke(Color.DARKRED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
						BorderWidths.DEFAULT)));
			} else {
				setBorder(null);
			}
		});
	}

	/**
	 * Set the function that determines the field's validity.
	 * 
	 * @param validator The validator
	 */
	public void setValidator(Function<String, Boolean> validator) {
		this.validator = Objects.requireNonNull(validator);
	}

	public BooleanProperty validityProperty() {
		return validity;
	}

}
