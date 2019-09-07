package com.sandpolis.core.profile;

import com.sandpolis.core.instance.event.ParameterizedEvent;

public final class Events {

	public static final class ProfileOnlineEvent extends ParameterizedEvent<Profile> {
	}

	public static final class ProfileOfflineEvent extends ParameterizedEvent<Profile> {
	}

	private Events() {
	}
}
