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
package com.sandpolis.core.net;

import com.sandpolis.core.net.sock.AbstractSock;
import com.sandpolis.core.net.sock.Sock;

import io.netty.channel.Channel;

/**
 * A {@link Sock} used for unit testing only.
 */
public class UnitSock extends AbstractSock {

	private boolean connected = true;
	private boolean authenticated = false;

	public UnitSock(Channel channel) {
		super(channel);
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public boolean isAuthenticated() {
		return authenticated;
	}

	@Override
	public void authenticate() {
		authenticated = true;
	}

	@Override
	public void deauthenticate() {
		authenticated = false;
	}

	@Override
	public String getRemoteIP() {
		return "127.0.0.1";
	}
}
