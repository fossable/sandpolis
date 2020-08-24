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
package com.sandpolis.viewer.lifegem.view.main.list;

import java.util.Objects;
import java.util.function.Function;

import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.viewer.lifegem.JavaFxAttribute;
import com.sandpolis.viewer.lifegem.StateTree.FxProfile;
import com.sandpolis.viewer.lifegem.common.FxUtil;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;

/**
 * A {@link TableColumn} that takes its value from an {@link Attribute}.
 *
 * @since 5.0.0
 */
public class AttributeColumn extends TableColumn<FxProfile, Label> {

	private final Oid<?> oid;

	public AttributeColumn(Oid<?> oid) {
		this.oid = Objects.requireNonNull(oid);

		// Set header text
		if (FxUtil.getResources().containsKey(oid.toString()))
			setText(FxUtil.getResources().getObject(oid.toString()).toString());

		// Set header image
		// TODO

		// TODO get actual converters
		Function<JavaFxAttribute<?>, Node> iconConverter = a -> null;
		Function<JavaFxAttribute<?>, String> textConverter = a -> a.get() == null ? null : a.get().toString();

		setCellValueFactory(p -> {
			ObjectProperty<Label> label = new SimpleObjectProperty<>(new Label());
			JavaFxAttribute<?> attribute = null;// (Attribute<?>) p.getValue().test(oid);

			// Bind the graphic property to the attribute via the converter function
			label.get().graphicProperty().bind(Bindings.createObjectBinding(() -> {
				return iconConverter.apply(attribute);
			}, attribute));

			// Bind the text property to the attribute via the converter function
			label.get().textProperty().bind(Bindings.createObjectBinding(() -> {
				return textConverter.apply(attribute);
			}, attribute));

			return label;
		});
	}

	public Oid<?> getOid() {
		return oid;
	}
}
