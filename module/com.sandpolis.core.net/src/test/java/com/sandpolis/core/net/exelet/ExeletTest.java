//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.net.exelet;

import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.exelet.Exelet.Handler;

import io.netty.channel.embedded.EmbeddedChannel;

public abstract class ExeletTest {

	/**
	 * Test that all handler methods are unique in name.
	 *
	 * @param _class The {@link Exelet} to be tested
	 */
	protected void testNameUniqueness(Class<? extends Exelet> _class) {
		var handlers = Arrays.stream(_class.getDeclaredMethods()).filter(m -> m.getAnnotation(Handler.class) != null)
				.map(m -> m.getName()).collect(Collectors.toList());

		assertEquals(handlers.size(), handlers.stream().distinct().count());
	}

	protected ExeletContext context;

	protected void initTestContext() {
		context = new ExeletContext(ConnectionStore.create(new EmbeddedChannel()), MSG.newBuilder().build());
	}

}
