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
package com.sandpolis.core.profile.store;

import com.sandpolis.core.instance.event.ParameterizedEvent;

public final class Events {

	public static final class ProfileOnlineEvent extends ParameterizedEvent<Profile> {
	}

	public static final class ProfileOfflineEvent extends ParameterizedEvent<Profile> {
	}

	private Events() {
	}
}
