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
package com.sandpolis.core.util;

/**
 * This utility simplifies the handling of many types of IDs.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class IDUtil {

	/**
	 * Get a request ID for use in a message that requires a response.
	 *
	 * @return A new request ID
	 */
	public static int msg() {
		return RandUtil.nextInt(1, Integer.MAX_VALUE);
	}

	/**
	 * Generate a new filesystem handle ID.
	 *
	 * @return The FsHandle ID
	 */
	public static int fm() {
		return RandUtil.nextInt();
	}

	/**
	 * Generate a new stream ID.
	 *
	 * @return The stream ID
	 */
	public static int stream() {
		return RandUtil.nextInt();
	}

	/**
	 * Generate a new listener ID.
	 *
	 * @return The listener ID
	 */
	public static int listener() {
		return RandUtil.nextInt();
	}

	/**
	 * Generate a new group ID.
	 *
	 * @return The group ID
	 */
	public static long group() {
		return RandUtil.nextLong();
	}

	private IDUtil() {
	}
}
