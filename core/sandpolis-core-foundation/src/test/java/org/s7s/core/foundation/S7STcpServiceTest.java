//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S7STcpServiceTest {

	@Test
	@DisplayName("Check some open ports")
	void checkOpenPorts() throws IOException {

		try (ServerSocket socket = new ServerSocket(8923)) {
			assertTrue(S7STcpService.of(8923).checkPort("localhost"));
		}

		assertFalse(S7STcpService.of(8923).checkPort("localhost"));
	}

	@Test
	@DisplayName("Check some closed ports")
	void checkClosedPorts() throws IOException {
		assertFalse(S7STcpService.of(8923).checkPort("localhost"));
	}

	@Test
	@DisplayName("Check well-known service names")
	@Disabled
	void getServiceName() {
		assertEquals("ftp", S7STcpService.of(21).serviceName().get());
		assertEquals("ssh", S7STcpService.of(22).serviceName().get());
		assertEquals("http", S7STcpService.of(80).serviceName().get());
	}

}
