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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.netty.channel.nio.NioEventLoopGroup;

class DnsUtilTest {

	@BeforeAll
	static void configure() {
		ThreadStore.init(config -> {
			config.defaults.put("net.dns.resolver", new NioEventLoopGroup(1).next());
		});
	}

	@Test
	void testGetPort() throws InterruptedException, ExecutionException {
		assertTrue(DnsUtil.getPort("invalid123").isEmpty());
		assertTrue(DnsUtil.getPort("test.google.com").isEmpty());
		assertThrows(ExecutionException.class, () -> DnsUtil.getPort(""));
	}

}
