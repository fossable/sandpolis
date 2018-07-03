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
package com.sandpolis.core.net.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Core;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.handler.EventHandler;
import com.sandpolis.core.net.handler.ExecuteHandler;
import com.sandpolis.core.proto.net.MSG.Message;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * This class provides a base for other {@link ChannelInitializer}s which
 * configure a {@link ChannelPipeline} for use as a specific connection type
 * (server, client, or viewer).
 * 
 * @see PeerPipelineInit
 * @see ClientPipelineInit
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class PipelineInitializer extends ChannelInitializer<Channel> {

	private static final Logger log = LoggerFactory.getLogger(PipelineInitializer.class);

	/**
	 * How often to compute traffic statistics in milliseconds.
	 */
	private static final long TRAFFIC_INTERVAL = 4000;

	/**
	 * The global protocol buffer frame encoder.
	 */
	protected static final ProtobufVarint32LengthFieldPrepender PROTO_FRAME_ENCODER = new ProtobufVarint32LengthFieldPrepender();

	/**
	 * The global protocol buffer decoder.
	 */
	protected static final ProtobufDecoder PROTO_DECODER = new ProtobufDecoder(Message.getDefaultInstance());

	/**
	 * The global protocol buffer encoder.
	 */
	protected static final ProtobufEncoder PROTO_ENCODER = new ProtobufEncoder();

	/**
	 * The global logging handler.
	 */
	protected static final LoggingHandler LOGGING = new LoggingHandler(Sock.class);

	/**
	 * The global connection logistics handler.
	 */
	protected static final EventHandler EVENT = new EventHandler();

	/**
	 * The {@link Exelet}s to use when initializing a {@link ExecuteHandler}.
	 */
	private Class<? extends Exelet>[] exelets;

	/**
	 * Construct a new {@link PipelineInitializer}.
	 * 
	 * @param exelets
	 *            Classes that will be scanned to build a {@link ExecuteHandler}
	 */
	@SuppressWarnings("unchecked")
	public PipelineInitializer(Class<? extends Exelet>[] exelets) {
		if (exelets == null)
			exelets = new Class[0];

		this.exelets = exelets;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		log.debug("Initializing a new Channel: {}", ch.id());

		ChannelTrafficShapingHandler traffic = new ChannelTrafficShapingHandler(TRAFFIC_INTERVAL);
		p.addLast("traffic", traffic);
		ch.attr(ChannelConstant.HANDLER_TRAFFIC).set(traffic);

		if (Core.LOG_NET_RAW)
			p.addLast(LOGGING);

		p.addLast(EVENT);

		p.addLast(new ProtobufVarint32FrameDecoder());
		p.addLast(PROTO_DECODER);
		p.addLast(PROTO_FRAME_ENCODER);
		p.addLast(PROTO_ENCODER);

		if (Core.LOG_NET)
			p.addLast(LOGGING);

		// TODO add EventExecutorGroup!
		ExecuteHandler execute = new ExecuteHandler(exelets);
		p.addLast("exe", execute);
		ch.attr(ChannelConstant.HANDLER_EXECUTE).set(execute);

	}

}
