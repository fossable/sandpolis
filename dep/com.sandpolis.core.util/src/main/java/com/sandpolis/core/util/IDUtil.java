/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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
package com.sandpolis.core.util;

import java.util.Objects;

import com.sandpolis.core.proto.util.Platform.Instance;

/**
 * This utility simplifies the handling of many types of IDs.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class IDUtil {

	/**
	 * A CVID is a 32 bit ID that uniquely identifies an instance on a particular
	 * server. The name comes from Client/Viewer ID, but server instances have one
	 * as well. Every instance is assigned a new CVID each time it connects to the
	 * server. Therefore CVIDs are suitible for identifying instances during the
	 * session only.
	 * 
	 * <pre>
	 * CVID Anatomy: [           Base CVID         |IID]
	 * </pre>
	 * 
	 * Each CVID has an Instance ID (IID) which encodes the instance type and a Base
	 * CVID which uniquely identifies the instance.<br>
	 * <br>
	 * Note: 0 is not a valid CVID and its presence in a CVID field indicates "N/A".
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	public static final class CVID {

		/**
		 * The number of bits used to encode the instance ID (IID).
		 */
		public static final int IID_SPACE = 3;

		/**
		 * Extract the instance type from a CVID.
		 * 
		 * @param cvid A CVID
		 * @return The cvid's instance
		 */
		public static Instance extractInstance(int cvid) {
			int iid = cvid & ((1 << IID_SPACE) - 1);
			return Instance.forNumber(iid);
		}

		/**
		 * Generate a new random CVID.<br>
		 * <br>
		 * Note: there's a small chance that this method will produce an invalid ID of 0
		 * for Charcoal instances. Since Charcoal is for debugging only, this is not
		 * remedied by introducing a validity-checking loop. Charcoal instances should
		 * manually check the output of this method and regenerate if equal to 0.
		 * 
		 * @param instance The new CVID's instance type
		 * @return A new CVID
		 */
		public static int cvid(Instance instance) {
			Objects.requireNonNull(instance);
			if (instance == Instance.UNRECOGNIZED)
				throw new IllegalArgumentException();

			return ((RandUtil.nextInt() << IID_SPACE) | instance.getNumber()) & 0x7FFFFFFF;
		}

		private CVID() {
		}
	}

	/**
	 * The maximum message ID.
	 */
	private static final int MSG_MAX = 64;

	private static int msg;

	/**
	 * Get an ID for use in a message which requires a response. IDs cycle from 0 to
	 * {@link #MSG_MAX} which saves a few bytes on the wire.
	 * 
	 * @return An ID ranging from 0-{@link #MSG_MAX}
	 */
	public static int msg() {
		msg++;
		if (msg > MSG_MAX) {
			msg = 0;
		}
		return msg;
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
