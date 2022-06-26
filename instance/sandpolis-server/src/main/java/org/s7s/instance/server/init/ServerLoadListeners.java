//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.init;

import static org.s7s.core.server.listener.ListenerStore.ListenerStore;

import org.s7s.core.instance.InitTask;

public class ServerLoadListeners extends InitTask {

	@Override
	public TaskOutcome run(TaskOutcome.Factory outcome) throws Exception {
		ListenerStore.start();

		return outcome.succeeded();
	}

	@Override
	public String description() {
		return "Load socket listeners";
	}

}
