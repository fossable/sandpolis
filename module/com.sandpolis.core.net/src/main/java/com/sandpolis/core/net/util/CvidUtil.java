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
package com.sandpolis.core.net.util;

import java.util.Objects;

import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.RandUtil;

/**
 * A CVID is a positive 32-bit ID that uniquely identifies an instance on a
 * Sandpolis network. The name historically stands for Client/Viewer ID, but
 * server instances have one as well. CVIDs are suitible for identifying
 * instances during a session only. For a long-term ID, use UUID.
 *
 * <pre>
 *                0         1         2           3
 *                012345678901234567890123 45678 901
 * CVID Anatomy: [0       Base CVID       | FID |IID]
 * </pre>
 *
 * Every CVID has an Instance ID (IID) that encodes the instance type, a Flavor
 * ID (FID) that encodes the instance flavor, and a Base CVID which uniquely
 * identifies the instance.<br>
 * <br>
 * Note: 0 is not a valid CVID and its presence in a CVID field indicates "N/A".
 *
 * @author cilki
 * @since 5.0.0
 */
public final class CvidUtil {

	/**
	 * The number of bits used to encode the instance ID (IID).
	 */
	public static final int IID_SPACE = 3;

	/**
	 * The number of bits used to encode the instance flavor ID (FID).
	 */
	public static final int FID_SPACE = 5;

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
	 * Extract the instance flavor from a CVID.
	 *
	 * @param cvid A CVID
	 * @return The cvid's instance flavor
	 */
	public static InstanceFlavor extractInstanceFlavor(int cvid) {
		int fid = (cvid >> IID_SPACE) & ((1 << FID_SPACE) - 1);
		return InstanceFlavor.forNumber(fid);
	}

	/**
	 * Generate a new random CVID.
	 *
	 * @param instance The new CVID's instance type
	 * @return A new CVID
	 */
	public static int cvid(Instance instance) {
		return cvid(instance, InstanceFlavor.NONE);
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
	 * @param flavor   The new CVID's instance flavor
	 * @return A new CVID
	 */
	public static int cvid(Instance instance, InstanceFlavor flavor) {
		Objects.requireNonNull(instance);
		Objects.requireNonNull(flavor);
		if (instance == Instance.UNRECOGNIZED)
			throw new IllegalArgumentException("Unrecognized instance type");
		if (flavor == InstanceFlavor.UNRECOGNIZED)
			throw new IllegalArgumentException("Unrecognized instance flavor");

		int r = RandUtil.nextInt();

		// Add InstanceFlavor ID
		r = ((r << FID_SPACE) | flavor.getNumber());

		// Add Instance ID
		r = ((r << IID_SPACE) | instance.getNumber());

		// Ensure positive
		return r & 0x7FFFFFFF;
	}

	private CvidUtil() {
	}
}
