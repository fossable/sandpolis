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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.ConfigConstant.logging;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.handler.ExeletHandler;
import com.sandpolis.core.net.handler.ManagementHandler;
import com.sandpolis.core.net.handler.ResponseHandler;
import com.sandpolis.core.net.handler.ShortcutFrameEncoder;
import com.sandpolis.core.net.handler.cvid.AbstractCvidHandler;
import com.sandpolis.core.net.handler.sand5.Sand5Handler;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.MSG.Message;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * This class provides a base for other {@link ChannelInitializer}s which
 * configure a {@link ChannelPipeline} for use as a specific connection type
 * (server, client, or viewer).
 *
 * @see PeerChannelInitializer
 * @see ClientChannelInitializer
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class AbstractChannelInitializer extends ChannelInitializer<Channel> {

	private static final Logger log = LoggerFactory.getLogger(AbstractChannelInitializer.class);

	public static final HandlerKey<ChannelTrafficShapingHandler> TRAFFIC = new HandlerKey<>("TrafficHandler");
	public static final HandlerKey<ProtobufVarint32FrameDecoder> FRAME_DECODER = new HandlerKey<>("FrameDecoder");
	public static final HandlerKey<ShortcutFrameEncoder> FRAME_ENCODER = new HandlerKey<>("FrameEncoder");
	public static final HandlerKey<ProtobufDecoder> PROTO_DECODER = new HandlerKey<>("ProtoDecoder");
	public static final HandlerKey<ProtobufEncoder> PROTO_ENCODER = new HandlerKey<>("ProtoEncoder");
	public static final HandlerKey<ManagementHandler> MANAGEMENT = new HandlerKey<>("EventHandler");
	public static final HandlerKey<SslHandler> SSL = new HandlerKey<>("SslHandler");
	public static final HandlerKey<AbstractCvidHandler> CVID = new HandlerKey<>("CvidHandler");
	public static final HandlerKey<ResponseHandler> RESPONSE = new HandlerKey<>("ResponseHandler");
	public static final HandlerKey<ExeletHandler> EXELET = new HandlerKey<>("ExeletHandler");
	public static final HandlerKey<Sand5Handler> SAND5 = new HandlerKey<>("Sand5Handler");
	public static final HandlerKey<LoggingHandler> LOG_RAW = new HandlerKey<>("RawLogHandler");
	public static final HandlerKey<LoggingHandler> LOG_DECODED = new HandlerKey<>("DecodedLogHandler");

	/**
	 * The global protocol buffer decoder.
	 */
	private static final ProtobufDecoder HANDLER_PROTO_DECODER = new ProtobufDecoder(Message.getDefaultInstance());

	/**
	 * The global protocol buffer encoder.
	 */
	private static final ProtobufEncoder HANDLER_PROTO_ENCODER = new ProtobufEncoder();

	/**
	 * The global logging handler.
	 */
	private static final LoggingHandler HANDLER_LOGGING = new LoggingHandler(Sock.class);

	/**
	 * The global connection logistics handler.
	 */
	private static final ManagementHandler HANDLER_MANAGEMENT = new ManagementHandler();

	/**
	 * The specification of the pipeline order.
	 */
	private final HandlerKey<?>[] pipelineSpec;

	protected AbstractChannelInitializer(HandlerKey<?>... pipelineSpec) {
		this.pipelineSpec = pipelineSpec;
	}

	protected AbstractChannelInitializer() {
		this(TRAFFIC, SSL, LOG_RAW, FRAME_DECODER, PROTO_DECODER, FRAME_ENCODER, PROTO_ENCODER, LOG_DECODED, CVID,
				RESPONSE, EXELET, MANAGEMENT);
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		log.debug("Initializing a new Channel: {}", ch.id());
		ChannelPipeline p = ch.pipeline();

		// Traffic management handler
		engage(p, TRAFFIC, new ChannelTrafficShapingHandler(Config.getInteger("traffic.interval")));

		if (Config.getBoolean(logging.net.traffic.raw))
			engage(p, LOG_RAW, HANDLER_LOGGING);

		//
		engage(p, FRAME_DECODER, new ProtobufVarint32FrameDecoder());
		engage(p, PROTO_DECODER, HANDLER_PROTO_DECODER);
		engage(p, FRAME_ENCODER, new ShortcutFrameEncoder());
		engage(p, PROTO_ENCODER, HANDLER_PROTO_ENCODER);

		if (Config.getBoolean(logging.net.traffic.decoded))
			engage(p, LOG_DECODED, HANDLER_LOGGING);

		// Sock management handler
		engage(p, MANAGEMENT, HANDLER_MANAGEMENT);
	}

	public <E extends ChannelHandler> void engage(ChannelPipeline pipeline, HandlerKey<E> type, E handler) {
		for (int i = Arrays.asList(pipelineSpec).indexOf(type) - 1; i >= 0; i--) {
			if (pipeline.names().contains(pipelineSpec[i].toString())) {
				pipeline.addAfter(pipelineSpec[i].toString(), type.toString(), handler);
				return;
			}
		}

		pipeline.addFirst(type.toString(), handler);
	}

	public <E extends ChannelHandler> void engage(ChannelPipeline pipeline, HandlerKey<E> type, E handler,
			EventExecutorGroup group) {
		for (int i = Arrays.asList(pipelineSpec).indexOf(type) - 1; i >= 0; i--) {
			if (pipeline.names().contains(pipelineSpec[i].toString())) {
				pipeline.addAfter(group, pipelineSpec[i].toString(), type.toString(), handler);
				return;
			}
		}

		pipeline.addFirst(group, type.toString(), handler);
	}
}
