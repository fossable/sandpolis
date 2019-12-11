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
package com.sandpolis.core.net.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.UnitSock;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.proto.net.Message.MSG;

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
		var channel = new EmbeddedChannel();
		channel.attr(ChannelConstant.CVID).set(0);
		channel.attr(ChannelConstant.UUID).set("123");

		context = new ExeletContext(new UnitSock(channel), MSG.newBuilder().build());
	}

}
