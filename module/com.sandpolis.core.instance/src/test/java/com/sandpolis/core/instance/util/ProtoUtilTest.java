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
package com.sandpolis.core.instance.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.net.Message.MSG;
import com.sandpolis.core.proto.net.MsgLogin.RQ_Login;
import com.sandpolis.core.proto.net.MsgServer.RS_ServerBanner;
import com.sandpolis.core.proto.util.Result.Outcome;

class ProtoUtilTest {

	@Test
	void testSetPayload() {
		RQ_Login.Builder payload1 = RQ_Login.newBuilder().setUsername("test").setPassword("pass");
		MSG message1 = MSG.newBuilder().setRqLogin(payload1).build();
		Outcome.Builder payload2 = Outcome.newBuilder().setComment("test").setResult(true);
		MSG message2 = MSG.newBuilder().setRsOutcome(payload2).build();
		RS_ServerBanner.Builder payload3 = RS_ServerBanner.newBuilder().setBanner("test");
		MSG message3 = MSG.newBuilder().setRsServerBanner(payload3).build();

		assertEquals(message1, ProtoUtil.setPayload(MSG.newBuilder(), payload1).build());
		assertEquals(message1, ProtoUtil.setPayload(MSG.newBuilder(), payload1.build()).build());
		assertEquals(message2, ProtoUtil.setPayload(MSG.newBuilder(), payload2).build());
		assertEquals(message2, ProtoUtil.setPayload(MSG.newBuilder(), payload2.build()).build());
		assertEquals(message3, ProtoUtil.setPayload(MSG.newBuilder(), payload3).build());
		assertEquals(message3, ProtoUtil.setPayload(MSG.newBuilder(), payload3.build()).build());
	}
}
