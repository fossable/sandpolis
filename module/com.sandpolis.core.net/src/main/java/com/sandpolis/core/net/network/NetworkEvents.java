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
package com.sandpolis.core.net.network;

import com.sandpolis.core.instance.store.event.ParameterizedEvent;

public final class NetworkEvents {

	public static final class ServerLostEvent extends ParameterizedEvent<Integer> {
		public ServerLostEvent(Integer cvid) {
			super(cvid);
		}
	}

	public static final class ServerEstablishedEvent extends ParameterizedEvent<Integer> {
		public ServerEstablishedEvent(Integer cvid) {
			super(cvid);
		}
	}

	public static final class CvidChangedEvent extends ParameterizedEvent<Integer> {
		public CvidChangedEvent(Integer cvid) {
			super(cvid);
		}
	}

	private NetworkEvents() {
	}
}
