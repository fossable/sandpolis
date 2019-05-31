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

import java.util.List;

import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.viewer.jfx.common.event.Event;
import com.sandpolis.viewer.jfx.common.event.ParameterizedEvent;

import javafx.scene.layout.Region;

public final class Events {

	/**
	 * Close the main menu.
	 */
	public static class MenuCloseEvent extends Event {
	}

	/**
	 * Open the main menu with the given {@link Region}.
	 */
	public static class MenuOpenEvent extends ParameterizedEvent<Region> {
	}

	/**
	 * Close the host detail.
	 */
	public static class HostDetailCloseEvent extends Event {
	}

	/**
	 * Open the host detail.
	 */
	public static class HostDetailOpenEvent extends ParameterizedEvent<Profile> {
	}

	/**
	 * Change the headers in the host list.
	 */
	public static class HostListHeaderChangeEvent extends ParameterizedEvent<List<AttributeKey<?>>> {
	}

	/**
	 * Open the auxiliary detail.
	 */
	public static class AuxDetailOpenEvent extends ParameterizedEvent<String> {
	}

	/**
	 * Close the auxiliary detail.
	 */
	public static class AuxDetailCloseEvent extends ParameterizedEvent<String> {
	}

	/**
	 * Change the primary view.
	 */
	public static class ViewChangeEvent extends ParameterizedEvent<String> {
	}

}
