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
package com.sandpolis.viewer.lifegem.view.generator.config_tree;

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
