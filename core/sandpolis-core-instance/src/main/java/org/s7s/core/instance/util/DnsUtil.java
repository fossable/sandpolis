//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.util;

import static org.s7s.core.instance.thread.ThreadStore.ThreadStore;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.s7s.core.foundation.S7SString;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;

/**
 * Utilities for DNS resolution.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class DnsUtil {

	/**
	 * An independent resolver for this class.
	 */
	private static final DnsNameResolver RESOLVER = new DnsNameResolverBuilder(ThreadStore.get("net.dns.resolver"))
			.channelFactory(() -> new NioDatagramChannel()).build();

	/**
	 * Get the port associated with an SRV record.
	 *
	 * @param server The DNS name
	 * @return The SRV port
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	// TODO Convert to async when Netty introduces future-mapping
	public static Optional<Integer> getPort(String server) throws InterruptedException, ExecutionException {
		Objects.requireNonNull(server);

		DnsQuestion question = new DefaultDnsQuestion(server, DnsRecordType.SRV);
		DnsResponse response = RESOLVER.query(question).get().content();
		DnsRawRecord record = response.recordAt(DnsSection.ANSWER);
		if (record == null)
			return Optional.empty();

		ByteBuf buffer = record.content();

		// Skip priority
		buffer.readShort();

		// Skip weight
		buffer.readShort();

		// Read port
		int port = buffer.readShort();
		if (!S7SString.of(port).isPort())
			return Optional.empty();

		return Optional.of(port);
	}

	/**
	 * Resolve a DNS A record.
	 *
	 * @param server The DNS name
	 * @return A future that will be notified with a host address
	 */
	// TODO Map InetAddress::getHostAddress when Netty introduces future-mapping
	public static Future<InetAddress> resolve(String server) {
		Objects.requireNonNull(server);

		return RESOLVER.resolve(server);
	}

	private DnsUtil() {
	}
}
