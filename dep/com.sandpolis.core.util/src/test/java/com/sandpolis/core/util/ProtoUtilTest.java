/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.net.MCLogin.RQ_Login;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Result.Outcome;

class ProtoUtilTest {

	@Test
	void testRsDynamicPayload() {
		RQ_Login.Builder payload = RQ_Login.newBuilder().setUsername("test").setPassword("pass");
		Message message = Message.newBuilder().setRqLogin(payload).build();
		assertEquals(message, ProtoUtil.rs(Message.newBuilder().build(), payload).build());
		assertEquals(message, ProtoUtil.rs(Message.newBuilder().build(), payload.build()).build());
	}

	@Test
	void testRsDynamicPayloadOutcome() {
		// Test special case
		Outcome.Builder payload = Outcome.newBuilder().setComment("test").setResult(true);
		Message message = Message.newBuilder().setRsOutcome(payload).build();
		assertEquals(message, ProtoUtil.rs(Message.newBuilder().build(), payload).build());
		assertEquals(message, ProtoUtil.rs(Message.newBuilder().build(), payload.build()).build());
	}

}
