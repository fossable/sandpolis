//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop;

import org.s7s.instance.client.desktop.init.LifegemLoadStores;
import org.s7s.instance.client.desktop.init.LifegemLoadUserInterface;
import org.s7s.core.instance.Entrypoint;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.init.InstanceLoadPlugins;

public final class Main extends Entrypoint {
	private Main(String[] args) {
		super(Main.class, InstanceType.CLIENT, InstanceFlavor.CLIENT_DESKTOP);

		register(new LifegemLoadStores());
		register(new InstanceLoadPlugins());
		register(new LifegemLoadUserInterface());

		start("Sandpolis Desktop Client", args);
	}

	public static void main(String[] args) {
		new Main(args);
	}

}
