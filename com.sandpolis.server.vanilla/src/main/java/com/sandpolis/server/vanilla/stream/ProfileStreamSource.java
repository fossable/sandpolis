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
package com.sandpolis.server.vanilla.stream;

import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.profile.ProfileStore.ProfileStore;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.store.connection.Events.SockLostEvent;
import com.sandpolis.core.profile.Events.ProfileOnlineEvent;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.core.proto.net.MCStream.ProfileStreamData;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.stream.store.StreamSource;

/**
 * Represents the origin of a profile stream. This source can be safely stopped
 * and restarted.
 *
 * @author cilki
 * @since 5.0.2
 */
public class ProfileStreamSource extends StreamSource<ProfileStreamData> {

	@Override
	public void stop() {
		ProfileStore.unregister(this);
		ConnectionStore.unregister(this);
	}

	@Override
	public void start() {
		ProfileStore.register(this);
		ConnectionStore.register(this);

		// TODO temporary
		// Send existing profiles
		ProfileStore.stream().filter(profile -> profile.getInstance() == Instance.CLIENT)
				.forEach(this::onProfileOnline);
	}

	private void onProfileOnline(Profile profile) {
		var data = ProfileStreamData.newBuilder().setCvid(profile.getCvid()).setUuid(profile.getUuid()).setOnline(true);

		ConnectionStore.get(profile.getCvid()).ifPresent(sock -> {
			data.setIp(sock.getRemoteIP());
			submit(data.build());
		});
	}

	@Subscribe
	private void onProfileOnline(ProfileOnlineEvent event) {
		onProfileOnline(event.get());
	}

	@Subscribe
	private void onSockLost(SockLostEvent event) {
		Sock sock = event.get();
		submit(ProfileStreamData.newBuilder().setCvid(sock.getRemoteCvid()).setUuid(sock.getRemoteUuid())
				.setOnline(false).build());
	}
}
