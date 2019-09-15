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
package com.sandpolis.core.instance.store.plugin;

import com.sandpolis.core.instance.event.ParameterizedEvent;

public final class Events {

	/**
	 * Indicates that a plugin was just loaded successfully.
	 */
	public static final class PluginLoadedEvent extends ParameterizedEvent<Plugin> {
	}

	public static final class PluginUnloadedEvent extends ParameterizedEvent<Plugin> {
	}

	private Events() {
	}
}
