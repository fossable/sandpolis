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

	public static final AttributeKey<Boolean> AUTH_STATE = AttributeKey.valueOf("state.auth");

	/**
	 * The remote host's SSL certificate status.
	 */
	public static final AttributeKey<Boolean> CERTIFICATE_STATE = AttributeKey.valueOf("state.certificate");

	/**
	 * The remote host's CVID.
	 */
	public static final AttributeKey<Integer> CVID = AttributeKey.valueOf("cvid");

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
	 * The remote host's UUID.
	 */
	public static final AttributeKey<String> UUID = AttributeKey.valueOf("uuid");

	private ChannelConstant() {
	}
}
