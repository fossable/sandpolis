package com.sandpolis.core.net.store.connection;

import com.sandpolis.core.instance.event.ParameterizedEvent;
import com.sandpolis.core.net.Sock;

public final class Events {

	public static final class SockLostEvent extends ParameterizedEvent<Sock> {
	}

	public static final class SockEstablishedEvent extends ParameterizedEvent<Sock> {
	}

	private Events() {
	}
}
