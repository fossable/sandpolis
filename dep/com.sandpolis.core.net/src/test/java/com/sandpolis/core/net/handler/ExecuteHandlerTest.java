/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.core.net.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.MessageFuture;
import com.sandpolis.core.proto.net.MCCvid.RQ_Cvid;
import com.sandpolis.core.proto.net.MCLogin.RQ_Login;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.net.MSG.Message.MsgOneofCase;

import io.netty.channel.embedded.EmbeddedChannel;

public class ExecuteHandlerTest {

	public static boolean rq_login_triggered;
	public static boolean rq_cvid_triggered;

	@BeforeEach
	public void setup() {
		rq_login_triggered = false;
		rq_cvid_triggered = false;
	}

	@Test
	public void testUnauth() {
		ExecuteHandler execute = new ExecuteHandler(new Class[] { TestExe.class, Test2Exe.class });
		EmbeddedChannel channel = new EmbeddedChannel(execute);
		execute.initUnauth(new Sock(channel));

		assertTrue(execute.containsHandler(MsgOneofCase.RQ_LOGIN));
		assertFalse(execute.containsHandler(MsgOneofCase.RQ_CVID));
		channel.writeInbound(Message.newBuilder().build());
		assertFalse(rq_login_triggered);
		assertFalse(rq_cvid_triggered);
		channel.writeInbound(Message.newBuilder().setRqLogin(RQ_Login.newBuilder()).build());
		assertTrue(rq_login_triggered);
		channel.writeInbound(Message.newBuilder().setRqCvid(RQ_Cvid.newBuilder()).build());
		assertFalse(rq_cvid_triggered);
	}

	@Test
	public void testAuth() {
		ExecuteHandler execute = new ExecuteHandler(new Class[] { TestExe.class, Test2Exe.class });
		EmbeddedChannel channel = new EmbeddedChannel(execute);
		Sock sock = new Sock(channel);
		execute.initUnauth(sock);
		execute.initAuth(sock);

		assertTrue(execute.containsHandler(MsgOneofCase.RQ_LOGIN));
		assertTrue(execute.containsHandler(MsgOneofCase.RQ_CVID));
		channel.writeInbound(Message.newBuilder().build());
		assertFalse(rq_login_triggered);
		assertFalse(rq_cvid_triggered);
		channel.writeInbound(Message.newBuilder().setRqLogin(RQ_Login.newBuilder()).build());
		assertTrue(rq_login_triggered);
		assertFalse(rq_cvid_triggered);
		channel.writeInbound(Message.newBuilder().setRqCvid(RQ_Cvid.newBuilder()).build());
		assertTrue(rq_cvid_triggered);

	}

	@Test
	public void testResponse() throws InterruptedException, ExecutionException, TimeoutException {
		ExecuteHandler execute = new ExecuteHandler(new Class[] { TestExe.class, Test2Exe.class });
		EmbeddedChannel channel = new EmbeddedChannel(execute);

		MessageFuture future = new MessageFuture();
		execute.putResponseFuture(14, future);
		channel.writeInbound(Message.newBuilder().setId(14).build());
		assertTrue(future.get(100, TimeUnit.MILLISECONDS) != null);
		assertTrue(execute.getResponseCount() == 0);

	}

	// Sandbox Exelets
	public static class TestExe extends Exelet {

		public TestExe(Sock connector) {
			super(connector);
		}

		@Unauth
		public void rq_login(Message msg) {
			rq_login_triggered = true;
		}

	}

	public static class Test2Exe extends Exelet {

		public Test2Exe(Sock connector) {
			super(connector);
		}

		@Auth
		public void rq_cvid(Message msg) {
			rq_cvid_triggered = true;
		}

	}

}
