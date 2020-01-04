//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.viewer.lifegem.common.field;

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
