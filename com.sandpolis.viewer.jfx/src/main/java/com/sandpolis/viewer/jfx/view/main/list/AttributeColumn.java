/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.viewer.jfx.view.main.list;

import java.util.Objects;
import java.util.function.Function;

import com.sandpolis.core.profile.attribute.Attribute;
import com.sandpolis.core.profile.attribute.key.AttributeKey;
import com.sandpolis.core.profile.store.Profile;
import com.sandpolis.viewer.jfx.common.FxUtil;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;

/**
 * A {@link TableColumn} that takes its value from an {@link Attribute}.
 *
 * @author cilki
 * @since 5.0.0
 */
public class AttributeColumn extends TableColumn<Profile, Label> {

	/**
	 * The {@link AttributeKey} associated with this {@link AttributeColumn}.
	 */
	private AttributeKey<?> key;

	/**
	 * Get the associated {@link AttributeColumn}.
	 *
	 * @return The {@link AttributeKey} associated with this {@link AttributeColumn}
	 */
	public AttributeKey<?> getKey() {
		return key;
	}

	public AttributeColumn(AttributeKey<?> key) {
		this.key = Objects.requireNonNull(key);

		// Read and cache header text
		if (key.getObject("name") == null)
			if (FxUtil.getResources().containsKey("ak." + key.getPath()))
				key.putObject("name", FxUtil.getResources().getObject("ak." + key.getPath()));
		setText(key.getObject("name"));

		// Read and cache header image
		if (key.getObject("icon") == null)
			if (FxUtil.getResources().containsKey("ak." + key.getPath() + ".icon"))
				key.putObject("icon",
						new Image((String) FxUtil.getResources().getObject("ak." + key.getPath() + ".icon")));
		setGraphic(key.getObject("icon"));

		// Functions that control how attributes are mapped into Strings and Nodes
		Function<Attribute<?>, Node> iconConverter = key.containsObject("iconConverter")
				? key.getObject("iconConverter")
				: a -> null;
		Function<Attribute<?>, String> textConverter = key.containsObject("textConverter")
				? key.getObject("textConverter")
				: a -> a.get() == null ? null : a.get().toString();

		setCellValueFactory(p -> {
			ObjectProperty<Label> label = new SimpleObjectProperty<>(new Label());
//			Attribute<?> attribute = (ObservableAttribute<?>) p.getValue().getAttribute(key);
//
//			if (attribute instanceof ObservableAttribute) {
//				// Bind the graphic property to the attribute via the converter function
//				label.get().graphicProperty().bind(Bindings.createObjectBinding(() -> {
//					return iconConverter.apply(attribute);
//				}, (ObservableAttribute<?>) attribute));
//
//				// Bind the text property to the attribute via the converter function
//				label.get().textProperty().bind(Bindings.createObjectBinding(() -> {
//					return textConverter.apply(attribute);
//				}, (ObservableAttribute<?>) attribute));
//			} else {
//				// Unchanging value attribute
//				label.get().setText(textConverter.apply(attribute));
//				label.get().setGraphic(iconConverter.apply(attribute));
//			}

			return label;
		});
	}
}
