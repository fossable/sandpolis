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
package com.sandpolis.viewer.jfx.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;

import javafx.fxml.FXMLLoader;

/**
 * Miscellaneous JavaFX utilities.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class FxUtil {

	/**
	 * Load a FXML resource.
	 * 
	 * @param location The absolute location of a FXML resource
	 * @param parent   The parent controller
	 * @return The object hierarchy from the FXML
	 * @throws IOException If the requested resource is not found
	 */
	public static <E> E load(String location, AbstractController parent) throws IOException {
		Objects.requireNonNull(location);
		Objects.requireNonNull(parent);

		FXMLLoader loader = new FXMLLoader(FxUtil.class.getResource(location));
		E node = loader.load();

		AbstractController controller = loader.getController();
		if (controller != null)
			if (parent.getBus() == null)
				parent.register(controller);
			else
				controller.setBus(parent.getBus());

		return node;
	}

	/**
	 * Load a root FXML resource.
	 * 
	 * @param location   The absolute location of a FXML resource
	 * @param parameters A list of parameters that will be posted to the
	 *                   controller's {@link EventBus}
	 * @return The object hierarchy from the FXML
	 * @throws IOException If the requested resource is not found
	 */
	public static <E> E loadRoot(String location, Object... parameters) throws IOException {
		Objects.requireNonNull(location);

		FXMLLoader loader = new FXMLLoader(FxUtil.class.getResource(location));
		E node = loader.load();

		AbstractController controller = loader.getController();
		controller.setBus(new EventBus());

		Arrays.stream(parameters).forEach(controller.getBus()::post);
		return node;
	}

	private FxUtil() {
	}

}