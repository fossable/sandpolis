/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
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
