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