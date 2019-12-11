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
package com.sandpolis.viewer.jfx.view.login;

import com.sandpolis.core.instance.event.Event;

public final class Events {

	/**
	 * Begin the login attempt.
	 */
	public static class BeginLoginEvent extends Event {
	}

	/**
	 * Begin the server connection attempt.
	 */
	public static class BeginServerConnectEvent extends Event {
	}

}
