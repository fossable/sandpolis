//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.init;

import static org.s7s.core.server.group.GroupStore.GroupStore;
import static org.s7s.core.server.listener.ListenerStore.ListenerStore;
import static org.s7s.core.server.user.UserStore.UserStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.Group.GroupConfig;
import org.s7s.core.instance.InitTask;
import org.s7s.core.instance.Listener.ListenerConfig;
import org.s7s.core.instance.User.UserConfig;

public class ServerFirstTimeSetup extends InitTask {

	private static final Logger log = LoggerFactory.getLogger(ServerFirstTimeSetup.class);

	@Override
	public String description() {
		return "First time initialization";
	}

	@Override
	public TaskOutcome run(TaskOutcome.Factory outcome) throws Exception {
		boolean skipped = true;

		// Setup default users
		if (UserStore.getMetadata().getInitCount() == 1) {
			log.debug("Creating default users");
			UserStore.create(UserConfig.newBuilder().setUsername("admin").setPassword("password").build());
			skipped = false;
		}

		// Setup default listeners
		if (ListenerStore.getMetadata().getInitCount() == 1) {
			log.debug("Creating default listeners");
			ListenerStore.create(ListenerConfig.newBuilder().setPort(8768).setAddress("0.0.0.0").setOwner("admin")
					.setName("Default Listener").setEnabled(true).build());
			skipped = false;
		}

		// Setup default groups
		if (GroupStore.getMetadata().getInitCount() == 1) {
			log.debug("Creating default groups");
			GroupStore
					.create(GroupConfig.newBuilder().setName("Default Authentication Group").setOwner("admin").build());
			skipped = false;
		}

		if (skipped)
			return outcome.skipped();

		return outcome.succeeded();
	}

}
