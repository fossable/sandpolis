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
package com.sandpolis.server.vanilla.stream;

import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.profile.store.ProfileStore.ProfileStore;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.net.MsgStream.EV_ProfileStream;
import com.sandpolis.core.net.connection.Connection;
import com.sandpolis.core.net.connection.ConnectionStoreEvents.SockLostEvent;
import com.sandpolis.core.net.stream.StreamSource;
import com.sandpolis.core.profile.store.Events.ProfileOnlineEvent;
import com.sandpolis.core.profile.store.Profile;
import com.sandpolis.core.util.Platform.Instance;

/**
 * Represents the origin of a profile stream. This source can be safely stopped
 * and restarted.
 *
 * @author cilki
 * @since 5.0.2
 */
public class ProfileStreamSource extends StreamSource<EV_ProfileStream> {

	@Override
	public void stop() {
		ProfileStore.unregister(this);
		ConnectionStore.unregister(this);
	}

	@Override
	public void start() {
		ProfileStore.register(this);
		ConnectionStore.register(this);

		// Send existing profiles
		ProfileStore.stream().filter(profile -> profile.getInstanceType() == Instance.CLIENT)
				.forEach(this::onProfileOnline);
	}

	private void onProfileOnline(Profile profile) {
		var ev = EV_ProfileStream.newBuilder().setCvid(profile.getCvid()).setUuid(profile.getUuid());

		ConnectionStore.get(profile.getCvid()).ifPresent(sock -> {
			ev.setIp(sock.getRemoteAddress()).setOnline(true);
		});

		ev.setHostname(profile.instance().client().getHostname());
		ev.setPlatform(profile.instance().client().getOs());

		submit(ev.build());
	}

	@Subscribe
	private void onProfileOnline(ProfileOnlineEvent event) {
		onProfileOnline(event.get());
	}

	@Subscribe
	private void onSockLost(SockLostEvent event) {
		Connection sock = event.get();
		submit(EV_ProfileStream.newBuilder().setCvid(sock.getRemoteCvid()).setUuid(sock.getRemoteUuid())
				.setOnline(false).build());
	}
}
