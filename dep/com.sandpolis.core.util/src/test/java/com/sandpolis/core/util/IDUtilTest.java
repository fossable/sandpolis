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
package com.sandpolis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.IDUtil.CVID;

class IDUtilTest {

	@Test
	@DisplayName("Check for Instance ID overflows")
	void iid_1() {
		for (Instance instance : Instance.values())
			if (instance != Instance.UNRECOGNIZED)
				assertTrue(instance.getNumber() <= (1 << IDUtil.CVID.IID_SPACE) - 1,
						"Maximum ID exceeded: " + instance.getNumber());
	}

	@Test
	@DisplayName("Check for InstanceFlavor ID overflows")
	void fid_1() {
		for (InstanceFlavor flavor : InstanceFlavor.values())
			if (flavor != InstanceFlavor.UNRECOGNIZED)
				assertTrue(flavor.getNumber() <= (1 << IDUtil.CVID.FID_SPACE) - 1,
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
					int cvid = CVID.cvid(instance, flavor);
					assertTrue(0 < cvid, "Invalid CVID: " + cvid);
					assertEquals(instance, CVID.extractInstance(cvid));
					assertEquals(flavor, CVID.extractInstanceFlavor(cvid));
				}
			}
		}
	}

	/**
	 * Maximum message ID.
	 */
	private static final int MSG_MAX = 64;

	@Test
	@DisplayName("Ensure message ID never exceeds MSG_MAX")
	void msg_1() {
		for (int i = 0; i < 10000; i++) {
			assertTrue(IDUtil.msg() <= MSG_MAX);
		}
	}
}
