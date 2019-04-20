/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.core.instance;

/**
 * A {@link Store} is designed to provide extremely convenient access to
 * optionally-persistent objects with a static context. {@code Store}s cannot be
 * instantiated and may require external initialization before being used.<br>
 * <br>
 * This class is a marker class.
 * 
 * @author cilki
 * @since 4.0.0
 */
public abstract class Store {

	/**
	 * Indicates that a {@link Store} initializes itself and no other configuration
	 * is required.<br>
	 * <br>
	 * Recommended usage:
	 * 
	 * <pre>
	 * {@code @AutoInitializer}
	 * class ExampleStore extends Store {
	 * 
	 *     static {
	 *         init();
	 *     }
	 * 
	 *     // Initialize or reinitialize the Store
	 *     public static void init() {
	 * 
	 *     }
	 * }
	 * </pre>
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	public static @interface AutoInitializer {
	}

	/**
	 * Indicates that a {@link Store} must be manually initialized with either
	 * {@code load()} or {@code init()}.<br>
	 * <br>
	 * Recommended usage:
	 * 
	 * <pre>
	 * {@code @ManualInitializer}
	 * class ExampleStore extends Store {
	 * 
	 *     // Initialize or reinitialize the Store
	 *     public static void init() {
	 * 
	 *     }
	 *     
	 *     public static void load() {
	 * 
	 *     }
	 * }
	 * </pre>
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	public static @interface ManualInitializer {
	}

}
