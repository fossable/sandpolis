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
import com.sandpolis.core.proto.util.Platform.Instance;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * {@link AttributeKey} constants that are useful to {@link Sock} and other
 * classes that use {@link Channel}s.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ChannelConstant {
	private ChannelConstant() {
	}

	/**
	 * The remote host's SSL certificate status.
	 */
	public static final AttributeKey<Boolean> CERTIFICATE_STATE = AttributeKey.valueOf("state.certificate");

	public static final AttributeKey<Boolean> AUTH_STATE = AttributeKey.valueOf("state.auth");

	/**
	 * The remote host's instance type.
	 */
	public static final AttributeKey<Instance> INSTANCE = AttributeKey.valueOf("instance");

	/**
	 * The {@link Sock} associated with the {@link Channel}.
	 */
	public static final AttributeKey<AbstractSock> SOCK = AttributeKey.valueOf("sock");

	/**
	 * Indicates whether an invalid SSL certificate will be allowed.
	 */
	public static final AttributeKey<Boolean> STRICT_CERTS = AttributeKey.valueOf("strictcerts");

	/**
	 * The remote host's CVID.
	 */
	public static final AttributeKey<Integer> CVID = AttributeKey.valueOf("cvid");

	/**
	 * The remote host's UUID.
	 */
	public static final AttributeKey<String> UUID = AttributeKey.valueOf("uuid");

}