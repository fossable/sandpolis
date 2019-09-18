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
package com.sandpolis.core.net.init;

import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.handler.peer.HolePunchHandler;
import com.sandpolis.core.net.handler.peer.PeerEncryptionDecoder;
import com.sandpolis.core.net.handler.peer.PeerEncryptionEncoder;
import com.sandpolis.core.net.sock.PeerSock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;

/**
 * This {@link AbstractChannelInitializer} configures a {@link Channel} for
 * peer-to-peer connections. Typically this initializer will be used with
 * datagram channels. The {@link Channel} will automatically perform a NAT
 * traversal prior to activation if required.
 *
 * @author cilki
 * @since 5.0.0
 */
public class PeerChannelInitializer extends AbstractChannelInitializer {

	public static final HandlerKey<PeerEncryptionEncoder> ENCRYPTION_ENCODER = new HandlerKey<>(
			"PeerEncryptionEncoder");
	public static final HandlerKey<PeerEncryptionDecoder> ENCRYPTION_DECODER = new HandlerKey<>(
			"PeerEncryptionDecoder");
	public static final HandlerKey<HolePunchHandler> HOLEPUNCHER = new HandlerKey<>("HolePunchHandler");

	public PeerChannelInitializer() {
		super(HOLEPUNCHER, TRAFFIC, ENCRYPTION_ENCODER, ENCRYPTION_DECODER, FRAME_DECODER, PROTO_DECODER, FRAME_ENCODER,
				PROTO_ENCODER, LOG_DECODED, RESPONSE, MANAGEMENT);
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		super.initChannel(ch);
		new PeerSock(ch);

		ChannelPipeline p = ch.pipeline();

		engage(p, ENCRYPTION_ENCODER, new PeerEncryptionEncoder());
		engage(p, ENCRYPTION_DECODER, new PeerEncryptionDecoder());

		if (ch instanceof DatagramChannel)
			engage(p, HOLEPUNCHER, new HolePunchHandler());
	}

}
