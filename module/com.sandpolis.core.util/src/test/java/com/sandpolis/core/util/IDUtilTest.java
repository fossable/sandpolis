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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

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

	@Test
	@DisplayName("Ensure message ID is positive")
	void msg_1() throws Exception {
		Field msg = IDUtil.class.getDeclaredField("msg");
		msg.setAccessible(true);
		msg.set(null, 0);

		assertTrue(IDUtil.msg() == 1);
		assertTrue(IDUtil.msg() == IDUtil.msg() - 1);
	}

	@Test
	@DisplayName("Ensure message ID wraps correctly")
	void msg_2() throws Exception {
		Field msg = IDUtil.class.getDeclaredField("msg");
		msg.setAccessible(true);
		msg.set(null, Integer.MAX_VALUE - 1);

		assertTrue(IDUtil.msg() == Integer.MAX_VALUE);
		assertTrue(IDUtil.msg() == 1);
	}
}
