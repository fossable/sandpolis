package com.sandpolis.core.net.store.network;

import com.sandpolis.core.instance.event.ParameterizedEvent;

public final class Events {

	public static final class ServerLostEvent extends ParameterizedEvent<Integer> {
	}

	public static final class ServerEstablishedEvent extends ParameterizedEvent<Integer> {
	}

	public static final class CvidChangedEvent extends ParameterizedEvent<Integer> {
	}

	private Events() {
	}
}
