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
package com.sandpolis.core.instance.util;

import static com.sandpolis.core.proto.util.Platform.InstanceFlavor.ASCETIC;
import static com.sandpolis.core.proto.util.Platform.InstanceFlavor.LIFEGEM;
import static com.sandpolis.core.proto.util.Platform.InstanceFlavor.LOCKSTONE;
import static com.sandpolis.core.proto.util.Platform.InstanceFlavor.MEGA;
import static com.sandpolis.core.proto.util.Platform.InstanceFlavor.SOAPSTONE;
import static com.sandpolis.core.proto.util.Platform.InstanceFlavor.VANILLA;

import java.util.function.BiConsumer;

import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

public class InstanceUtil {

	public static InstanceFlavor[] getFlavors(Instance instance) {
		switch (instance) {
		case CHARCOAL:
			return new InstanceFlavor[] {};
		case CLIENT:
			return new InstanceFlavor[] { MEGA };
		case INSTALLER:
			return new InstanceFlavor[] {};
		case SERVER:
			return new InstanceFlavor[] { VANILLA };
		case VIEWER:
			return new InstanceFlavor[] { ASCETIC, LIFEGEM, SOAPSTONE, LOCKSTONE };
		default:
			return null;
		}
	}

	public static void iterate(BiConsumer<Instance, InstanceFlavor> consumer) {
		for (var instance : Instance.values()) {
			if (instance != Instance.UNRECOGNIZED) {
				for (var flavor : getFlavors(instance)) {
					consumer.accept(instance, flavor);
				}
			}
		}
	}
}
