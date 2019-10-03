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
package com.sandpolis.core.net.handler.sand5;

import static com.sandpolis.core.instance.store.thread.ThreadStore.ThreadStore;

import java.util.Arrays;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * This handler implements the entire SAND5 challenge protocol.
 *
 * @author cilki
 * @since 5.0.2
 */
public class Sand5Handler extends SimpleChannelInboundHandler<ByteBuf> {

	private static final Logger log = LoggerFactory.getLogger(Sand5Handler.class);

	/**
	 * The size of the challenge nonce in bytes.
	 */
	private static final int CHALLENGE_SIZE = 128;

	/**
	 * Represents the {@link Sand5Handler}'s current phase.
	 */
	private static enum Phase {

		/**
		 * Indicates that the remote endpoint is being evaluated.
		 */
		VERIFY_REMOTE,

		/**
		 * Indicates that the local endpoint is being evaluated.
		 */
		VERIFY_LOCAL,

		/**
		 * Indicates that the procedure is complete (but not necessarily successfully).
		 */
		DONE;
	}

	/**
	 * Build a new {@link Sand5Handler} preconfigured to be used in request mode.
	 *
	 * @param key The keypair to use during the challenge
	 * @return The handler
	 */
	public static Sand5Handler newRequestHandler(ReciprocalKeyPair key) {
		return new Sand5Handler(key, Phase.VERIFY_REMOTE, Phase.VERIFY_LOCAL, Phase.DONE);
	}

	/**
	 * Build a new {@link Sand5Handler} preconfigured to be used in response mode.
	 *
	 * @param key The keypair to use during the challenge
	 * @return The handler
	 */
	public static Sand5Handler newResponseHandler(ReciprocalKeyPair key) {
		return new Sand5Handler(key, Phase.VERIFY_LOCAL, Phase.VERIFY_REMOTE, Phase.DONE);
	}

	/**
	 * A list containing the current {@link Phase} at the head.
	 */
	private final LinkedList<Phase> phase;

	/**
	 * The challenge nonce.
	 */
	private final byte[] challenge;

	/**
	 * The keypair to use for signing and verifying.
	 */
	private final ReciprocalKeyPair key;

	/**
	 * The entire operation's future.
	 */
	private final Promise<Boolean> future;

	/**
	 * Whether the remote endpoint was verified.
	 */
	private boolean remoteVerified;

	private Sand5Handler(ReciprocalKeyPair key, Phase... sequence) {
		this.phase = new LinkedList<>(Arrays.asList(sequence));
		this.key = key;
		this.challenge = CryptoUtil.getNonce(CHALLENGE_SIZE);
		this.future = new DefaultPromise<>(ThreadStore.get("net.message.incoming"));
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		switch (phase.peek()) {
		case VERIFY_LOCAL:
			byte[] nonce = new byte[msg.readableBytes()];
			msg.readBytes(nonce);
			ctx.writeAndFlush(Unpooled.wrappedBuffer(CryptoUtil.SAND5.sign(key, nonce)));
			break;
		case VERIFY_REMOTE:
			byte[] signed = new byte[msg.readableBytes()];
			msg.readBytes(signed);
			remoteVerified = CryptoUtil.SAND5.check(key, challenge, signed);
			break;
		default:
			throw new IllegalStateException();
		}

		// Advance the phase
		phase.pollFirst();

		// Check for completion
		if (phase.peek() == Phase.DONE) {
			// Autoremove
			ctx.pipeline().remove(this);

			future.setSuccess(remoteVerified);
		}

		// Check for VERIFY_REMOTE phase
		else if (phase.peek() == Phase.VERIFY_REMOTE) {
			ctx.writeAndFlush(Unpooled.wrappedBuffer(challenge));
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (phase.peek().equals(Phase.VERIFY_REMOTE))
			ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(challenge));

		super.channelActive(ctx);
	}

	/**
	 * Get the operation's {@link Future}.
	 *
	 * @return The challenge future
	 */
	public Future<Boolean> challengeFuture() {
		return future;
	}

}
