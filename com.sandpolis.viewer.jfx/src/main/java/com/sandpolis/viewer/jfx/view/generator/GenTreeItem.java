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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.image.ImageView;

/**
 * The superclass for all items in the generator's configuration tree.
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class GenTreeItem {

	enum Type {
		CATEGORY, GROUP, ATTRIBUTE;
	}

	private Type type;

	/**
	 * The name of this {@link GenTreeItem}.
	 */
	private StringProperty name = new SimpleStringProperty();

	/**
	 * The icon of this {@link GenTreeItem}.
	 */
	private ObjectProperty<Node> icon = new SimpleObjectProperty<>();

	public GenTreeItem(Type type, String name) {
		this.type = Objects.requireNonNull(type);
		this.name.set(Objects.requireNonNull(name));
	}

	public Type getType() {
		return type;
	}

	public StringProperty name() {
		return name;
	}

	public ObjectProperty<Node> icon() {
		return icon;
	}

	/**
	 * Set the {@link GenTreeItem}'s icon.
	 * 
	 * @param location The icon name within the icons directory
	 * @return {@code this}
	 */
	public GenTreeItem icon(String location) {
		this.icon.set(new ImageView("/image/icon16/common/" + Objects.requireNonNull(location)));
		return this;
	}
}