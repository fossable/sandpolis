//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.util;

import java.util.Objects;

import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.S7SRandom;

/**
 * A SID is a positive 32-bit ID that uniquely identifies an instance on a
 * Sandpolis network. The name historically stands for Client/Viewer ID, but
 * server instances have one as well. SIDs are suitable for identifying
 * instances during a session only. For a long-term ID, use UUID.
 *
 * <pre>
 *                0         1         2           3
 *                012345678901234567890123 45678 901
 * SID Anatomy:  [0        Base SID       | FID |IID]
 * </pre>
 *
 * Every SID has an Instance ID (IID) that encodes the instance type, a Flavor
 * ID (FID) that encodes the instance flavor, and a Base SID which uniquely
 * identifies the instance.<br>
 * <br>
 * Note: 0 is not a valid SID and its presence in a SID field indicates "N/A".
 *
 * @author cilki
 * @since 5.0.0
 */
public record S7SSessionID(int sid, InstanceType instanceType, InstanceFlavor instanceFlavor) {

	/**
	 * The number of bits used to encode the instance ID (IID).
	 */
	public static final int IID_SPACE = 3;

	/**
	 * The number of bits used to encode the instance flavor ID (FID).
	 */
	public static final int FID_SPACE = 5;

	public static S7SSessionID of(int sid) {
		return new S7SSessionID(sid, InstanceType.forNumber(sid & ((1 << IID_SPACE) - 1)),
				InstanceFlavor.forNumber((sid >> IID_SPACE) & ((1 << FID_SPACE) - 1)));
	}

	/**
	 * Generate a new random SID.
	 *
	 * @param instance The new SID's instance type
	 * @param flavor   The new SID's instance flavor
	 * @return A new SID
	 */
	public static S7SSessionID of(InstanceType instance, InstanceFlavor flavor) {
		Objects.requireNonNull(instance);
		Objects.requireNonNull(flavor);
		if (instance == InstanceType.UNRECOGNIZED)
			throw new IllegalArgumentException("Unrecognized instance type");
		if (flavor == InstanceFlavor.UNRECOGNIZED)
			throw new IllegalArgumentException("Unrecognized instance flavor");

		int r = S7SRandom.insecure.nextInt();

		// Add InstanceFlavor ID
		r = ((r << FID_SPACE) | flavor.getNumber());

		// Add Instance ID
		r = ((r << IID_SPACE) | instance.getNumber());

		// Ensure positive
		return new S7SSessionID(r & 0x7FFFFFFF, instance, flavor);
	}
}
