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
package com.sandpolis.core.net.util;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.instance.PoolConstant;

import io.netty.channel.nio.NioEventLoopGroup;

class DnsUtilTest {

	@BeforeAll
	static void configure() {
		ThreadStore.init(config -> {
			config.ephemeral();
			config.defaults.put(PoolConstant.net.dns.resolver, new NioEventLoopGroup(1).next());
		});
	}

	@Test
	void testGetPort() throws InterruptedException, ExecutionException {
		assertEquals((int) DnsUtil.getPort("test.sandpolis.com").get(), 12345);
		assertTrue(DnsUtil.getPort("invalid123").isEmpty());
		assertTrue(DnsUtil.getPort("test.google.com").isEmpty());
		assertThrows(ExecutionException.class, () -> DnsUtil.getPort(""));
	}

}
