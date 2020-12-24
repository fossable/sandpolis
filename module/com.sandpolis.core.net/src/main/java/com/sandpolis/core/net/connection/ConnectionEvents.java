//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.connection;

import com.sandpolis.core.instance.store.event.ParameterizedEvent;

public final class ConnectionEvents {

	public static final class SockLostEvent extends ParameterizedEvent<Connection> {
		public SockLostEvent(Connection connection) {
			super(connection);
		}
	}

	public static final class SockEstablishedEvent extends ParameterizedEvent<Connection> {
		public SockEstablishedEvent(Connection connection) {
			super(connection);
		}
	}

	private ConnectionEvents() {
	}
}
