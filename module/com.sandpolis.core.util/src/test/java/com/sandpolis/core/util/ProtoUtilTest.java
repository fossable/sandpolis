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
package com.sandpolis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.net.MCLogin.RQ_Login;
import com.sandpolis.core.proto.net.MCServer.RS_ServerBanner;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Result.Outcome;

class ProtoUtilTest {

	@Test
	void testSetPayload() {
		RQ_Login.Builder payload1 = RQ_Login.newBuilder().setUsername("test").setPassword("pass");
		Message message1 = Message.newBuilder().setRqLogin(payload1).build();
		Outcome.Builder payload2 = Outcome.newBuilder().setComment("test").setResult(true);
		Message message2 = Message.newBuilder().setRsOutcome(payload2).build();
		RS_ServerBanner.Builder payload3 = RS_ServerBanner.newBuilder().setBanner("test");
		Message message3 = Message.newBuilder().setRsServerBanner(payload3).build();

		assertEquals(message1, ProtoUtil.setPayload(Message.newBuilder(), payload1).build());
		assertEquals(message1, ProtoUtil.setPayload(Message.newBuilder(), payload1.build()).build());
		assertEquals(message2, ProtoUtil.setPayload(Message.newBuilder(), payload2).build());
		assertEquals(message2, ProtoUtil.setPayload(Message.newBuilder(), payload2.build()).build());
		assertEquals(message3, ProtoUtil.setPayload(Message.newBuilder(), payload3).build());
		assertEquals(message3, ProtoUtil.setPayload(Message.newBuilder(), payload3.build()).build());
	}
}
