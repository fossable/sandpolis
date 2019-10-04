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
package com.sandpolis.core.instance.plugin;

/**
 * A class that plugins can subclass to receive lifecycle events.
 *
 * @author cilki
 * @since 5.1.0
 */
public abstract class SandpolisPlugin {

	/**
	 * A lifecycle method that is called immediately after the plugin is loaded.
	 */
	public void loaded() {
	}

	/**
	 * A lifecycle method that is called immediately before the plugin is unloaded.
	 */
	public void unloaded() {
	}

}
