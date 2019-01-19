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
package com.sandpolis.viewer.jfx.common.event;

/**
 * An event that contains an {@link Object} parameter.
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class ParameterizedEvent<E> extends Event {

	/**
	 * The parameter which will be modified reflectively.
	 */
	private E object;

	/**
	 * Get the event's parameter.
	 * 
	 * @return The event parameter
	 */
	public E get() {
		return object;
	}

}
