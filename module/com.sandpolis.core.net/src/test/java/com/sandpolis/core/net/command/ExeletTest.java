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
package com.sandpolis.core.net.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.sandpolis.core.net.ChannelConstant;
import com.sandpolis.core.net.UnitSock;
import com.sandpolis.core.net.command.Exelet.Handler;
import com.sandpolis.core.net.handler.exelet.ExeletContext;

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

		context = new ExeletContext(new UnitSock(channel));
	}

}
