/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.net.init;

import java.util.concurrent.Future;

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.handler.ExecuteHandler;
import com.sandpolis.core.proto.util.Platform.Instance;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;

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
	public static final AttributeKey<Sock.CertificateState> CERTIFICATE_STATE = AttributeKey
			.valueOf("state.certificate");

	/**
	 * The remote host's connection status.
	 */
	public static final AttributeKey<Sock.ConnectionState> CONNECTION_STATE = AttributeKey.valueOf("state.connection");

	/**
	 * The {@link Future} that will be notified when the CVID handshake completes.
	 */
	public static final AttributeKey<Promise<Integer>> FUTURE_CVID = AttributeKey.valueOf("future.cvid");

	/**
	 * The remote host's {@link ExecuteHandler}.
	 */
	public static final AttributeKey<ExecuteHandler> HANDLER_EXECUTE = AttributeKey.valueOf("handler.execute");

	/**
	 * The remote host's {@link SslHandler} or {@code null} if SSL was not used.
	 */
	public static final AttributeKey<SslHandler> HANDLER_SSL = AttributeKey.valueOf("handler.ssl");

	/**
	 * The remote host's {@link ChannelTrafficShapingHandler}.
	 */
	public static final AttributeKey<ChannelTrafficShapingHandler> HANDLER_TRAFFIC = AttributeKey
			.valueOf("handler.traffic");

	/**
	 * The remote host's instance type.
	 */
	public static final AttributeKey<Instance> INSTANCE = AttributeKey.valueOf("instance");

	/**
	 * The {@link Sock} associated with the {@link Channel}.
	 */
	public static final AttributeKey<Sock> SOCK = AttributeKey.valueOf("sock");

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
