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

import java.util.List;

import com.sandpolis.core.instance.event.Event;
import com.sandpolis.core.instance.event.ParameterizedEvent;
import com.sandpolis.core.proto.net.MsgPlugin.PluginDescriptor;

public final class Events {

	/**
	 * Indicates that a login attempt has started.
	 */
	public static final class LoginStartedEvent extends Event {
	}

	/**
	 * Indicates that a login attempt has ended.
	 */
	public static final class LoginEndedEvent extends ParameterizedEvent<String> {
	}

	/**
	 * Indicates that a connection attempt has started.
	 */
	public static final class ConnectStartedEvent extends Event {
	}

	/**
	 * Indicates that a connection attempt has ended.
	 */
	public static final class ConnectEndedEvent extends ParameterizedEvent<String> {
	}

	public static final class PluginListEvent extends ParameterizedEvent<List<PluginDescriptor>> {
	}
}
