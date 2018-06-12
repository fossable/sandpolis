/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.viewer;

/**
 * A {@code UI} represents a user interface that can be started and stopped.
 * This interface allows the {@link Viewer} class to launch different types of
 * frontends. The frontend that implements this interface must be registered
 * with the viewer via {@code Viewer.registerUI} before
 * {@code MainDispatch.dispatch} is called.
 * 
 * @author cilki
 * @since 5.0.0
 */
public interface UI {

	/**
	 * Launch the user frontend. This method must be non-blocking.
	 * 
	 * @throws Exception
	 *             Any error that may occur during startup
	 */
	public void start() throws Exception;

	/**
	 * Close the user frontend. This method must be non-blocking.
	 * 
	 * @throws Exception
	 *             Any error that may occur during cleanup
	 */
	public void stop() throws Exception;

}
