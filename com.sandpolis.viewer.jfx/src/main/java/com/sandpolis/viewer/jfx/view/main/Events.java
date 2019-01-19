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
package com.sandpolis.viewer.jfx.view.main;

import com.sandpolis.viewer.jfx.common.event.ParameterizedEvent;
import com.sandpolis.viewer.jfx.common.event.Event;

import javafx.scene.layout.Pane;

public final class Events {

	/**
	 * Close the main menu.
	 */
	public static class MenuCloseEvent extends Event {
	}

	/**
	 * Open the main menu with the given {@link Pane}.
	 */
	public static class MenuOpenEvent extends ParameterizedEvent<Pane> {
	}

	/**
	 * Close the main detail.
	 */
	public static class DetailCloseEvent extends Event {
	}

	/**
	 * Open the main detail.
	 */
	public static class DetailOpenEvent extends ParameterizedEvent<String> {
	}

}
