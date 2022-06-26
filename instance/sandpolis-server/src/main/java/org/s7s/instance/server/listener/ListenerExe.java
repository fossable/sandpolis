//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.listener;

import static org.s7s.core.foundation.Instance.InstanceType.CLIENT;
import static org.s7s.core.server.listener.ListenerStore.ListenerStore;
import static org.s7s.core.server.user.UserStore.UserStore;

import org.s7s.core.protocol.Listener.RQ_CreateListener;
import org.s7s.core.protocol.Listener.RS_CreateListener;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;

/**
 * {@link ListenerExe} contains message handlers related to listener management.
 *
 * @since 5.0.0
 */
public final class ListenerExe extends Exelet {

	@Handler(auth = true, instances = CLIENT)
	public static RS_CreateListener rq_create_listener(ExeletContext context, RQ_CreateListener rq) {

		var user = UserStore.getBySid(context.connector.get(ConnectionOid.REMOTE_SID).asInt()).orElse(null);
		if (user == null)
			return RS_CreateListener.CREATE_LISTENER_ACCESS_DENIED;

		ListenerStore.create(listener -> {
			// TODO
		});

		return RS_CreateListener.CREATE_LISTENER_OK;
	}

	private ListenerExe() {
	}
}
