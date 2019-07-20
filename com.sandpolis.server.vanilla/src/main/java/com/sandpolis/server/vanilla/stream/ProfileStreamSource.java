/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.server.vanilla.stream;

import static com.sandpolis.core.net.store.connection.ConnectionStore.Events.SOCK_LOST;
import static com.sandpolis.core.profile.ProfileStore.Events.PROFILE_ONLINE;

import java.util.function.Consumer;

import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.store.connection.ConnectionStore;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.core.profile.ProfileStore;
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

	private final Consumer<Profile> online = (Profile profile) -> {
		submit(ProfileStreamData.newBuilder().setCvid(profile.getCvid()).setUuid(profile.getUuid())
				.setIp(ConnectionStore.get(profile.getCvid()).getRemoteIP()).setOnline(true).build());
	};

	private final Consumer<Sock> offline = (Sock sock) -> {
		submit(ProfileStreamData.newBuilder().setCvid(sock.getRemoteCvid()).setUuid(sock.getRemoteUuid())
				.setOnline(false).build());
	};

	@Override
	public void stop() {
		Signaler.remove(PROFILE_ONLINE, online);
		Signaler.remove(SOCK_LOST, offline);
	}

	@Override
	public void start() {
		Signaler.register(PROFILE_ONLINE, online);
		Signaler.register(SOCK_LOST, offline);

		// TODO temporary
		// Send existing profiles
		ProfileStore.getProfiles().filter(profile -> profile.getInstance() == Instance.CLIENT).forEach(online);
	}
}
