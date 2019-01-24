/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.core.net.util;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.sandpolis.core.instance.store.thread.ThreadStore;
import com.sandpolis.core.util.ValidationUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.resolver.dns.DnsNameResolverBuilder;

/**
 * DNS-related utilities.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class DnsUtil {

	/**
	 * Get the port associated with an SRV record.
	 * 
	 * @param server The DNS name
	 * @return The SRV port
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static Optional<Integer> getPort(String server) throws InterruptedException, ExecutionException {
		Objects.requireNonNull(server);

		DnsQuestion question = new DefaultDnsQuestion(server, DnsRecordType.SRV);
		DnsResponse response = new DnsNameResolverBuilder(ThreadStore.get("dns"))
				.channelFactory(() -> new NioDatagramChannel()).build().query(question).get().content();
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
		if (!ValidationUtil.port(port))
			return Optional.empty();

		return Optional.of(port);
	}

	private DnsUtil() {
	}
}
