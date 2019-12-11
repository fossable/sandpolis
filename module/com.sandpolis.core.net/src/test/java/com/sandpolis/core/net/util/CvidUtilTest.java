//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.net.util;

import static com.sandpolis.core.net.util.CvidUtil.cvid;
import static com.sandpolis.core.net.util.CvidUtil.extractInstance;
import static com.sandpolis.core.net.util.CvidUtil.extractInstanceFlavor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

class CvidUtilTest {

	@Test
	@DisplayName("Check for Instance ID overflows")
	void iid_1() {
		for (Instance instance : Instance.values())
			if (instance != Instance.UNRECOGNIZED)
				assertTrue(instance.getNumber() <= (1 << CvidUtil.IID_SPACE) - 1,
						"Maximum ID exceeded: " + instance.getNumber());
	}

	@Test
	@DisplayName("Check for InstanceFlavor ID overflows")
	void fid_1() {
		for (InstanceFlavor flavor : InstanceFlavor.values())
			if (flavor != InstanceFlavor.UNRECOGNIZED)
				assertTrue(flavor.getNumber() <= (1 << CvidUtil.FID_SPACE) - 1,
						"Maximum ID exceeded: " + flavor.getNumber());
	}

	@Test
	@DisplayName("Check a few random CVIDs for validity")
	void cvid_1() {
		for (Instance instance : Instance.values()) {
			for (InstanceFlavor flavor : InstanceFlavor.values()) {
				if (instance == Instance.UNRECOGNIZED || flavor == InstanceFlavor.UNRECOGNIZED)
					continue;

				for (int i = 0; i < 1000; i++) {
					int cvid = cvid(instance, flavor);
					assertTrue(0 < cvid, "Invalid CVID: " + cvid);
					assertEquals(instance, extractInstance(cvid));
					assertEquals(flavor, extractInstanceFlavor(cvid));
				}
			}
		}
	}

}
