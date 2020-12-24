//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.profile;

import com.sandpolis.core.instance.store.event.ParameterizedEvent;

public final class ProfileEvents {

	public static final class ProfileOnlineEvent extends ParameterizedEvent<Profile> {
		public ProfileOnlineEvent(Profile profile) {
			super(profile);
		}
	}

	public static final class ProfileOfflineEvent extends ParameterizedEvent<Profile> {
		public ProfileOfflineEvent(Profile profile) {
			super(profile);
		}
	}

	private ProfileEvents() {
	}
}
