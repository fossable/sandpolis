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
package com.sandpolis.core.net.handler;

import java.net.ProtocolException;

import com.sandpolis.core.util.RandUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ProgressivePromise;

/**
 * This handler ensures a UDP "connection" is bidirectional by repeatedly
 * pinging the remote host. If the host responds, the handler will wait a
 * "silence period" before activating the channel. This NAT traversal scheme is
 * classic "hole-punching".<br>
 * <br>
 * This handler should always be first in the pipeline and will automatically
 * remove itself once the hole is punched.
 *
 * @author cilki
 * @since 5.0.0
 */
public class HolePunchHandler extends ChannelInboundHandlerAdapter {

	/**
	 * The maximum number of requests to send before giving up.
	 */
	private static final int RQ_MAX = 3;

	/**
	 * The timeout for each request in milliseconds.
	 */
	private static final int RQ_TIMEOUT = 800;

	/**
	 * The amount of time to wait after a response has been received.
	 */
	private static final int SILENT_TIME = 800;

	/**
	 * The identifier that prefixes every request.
	 */
	private static final int RQ_MAGIC = 0xC390A7D5;

	/**
	 * The identifier that prefixes every response.
	 */
	private static final int RS_MAGIC = 0x5D7A093C;

	/**
	 * The last request payload that was sent to the remote host.
	 */
	private int lastRequest;

	private ProgressivePromise<Channel> handshakeFuture;

	/**
	 * The request thread.
	 */
	private Thread rq;

	private Thread silence;

	public HolePunchHandler() {
		handshakeFuture = GlobalEventExecutor.INSTANCE.newProgressivePromise();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {

		rq = new Thread(() -> {
			// Begin request loop
			try {
				for (int i = 0; i < RQ_MAX; i++) {
					lastRequest = RandUtil.nextInt();
					ctx.writeAndFlush(Unpooled.buffer(Integer.BYTES * 2).writeInt(RQ_MAGIC).writeInt(lastRequest));

					Thread.sleep(RQ_TIMEOUT);
				}
			} catch (InterruptedException e) {
				return;
			} finally {
				// Check status
				if (!handshakeFuture.isDone() && silence == null) {
					handshakeFuture.setSuccess(null);
				}
			}
		});
		rq.start();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buffer = (ByteBuf) msg;

		while (buffer.readableBytes() > 0)
			switch (buffer.readInt()) {
			case RQ_MAGIC:
				// A valid request has been received
				ctx.writeAndFlush(Unpooled.buffer(Integer.BYTES * 2).writeInt(RS_MAGIC).writeInt(buffer.readInt()));
				break;
			case RS_MAGIC:

				if (buffer.readInt() == lastRequest) {
					if (silence == null) {
						silence = new Thread(() -> {
							try {
								Thread.sleep(SILENT_TIME);
							} catch (InterruptedException e) {
								return;
							}

							// Autoremove the handler
							ctx.pipeline().remove(HolePunchHandler.this);

							// Activate the channel
							try {
								super.channelActive(ctx);
							} catch (Exception e) {
								return;
							}

							// Complete the handshake
							handshakeFuture.setSuccess(ctx.channel());
						});

						rq.interrupt();
						silence.start();
					}
				}
				break;
			default:
				// Invalid message
				handshakeFuture.setFailure(new ProtocolException("Invalid message received during handshake"));
				if (rq != null)
					rq.interrupt();
				if (silence != null)
					silence.interrupt();
				return;
			}

	}

	/**
	 * Get a future which completes once the handshake is complete. If the future's
	 * result is null, the handshake was not able to punch a hole.
	 *
	 * @return The handshake future
	 */
	public Future<Channel> handshakeFuture() {
		return handshakeFuture;
	}
}
