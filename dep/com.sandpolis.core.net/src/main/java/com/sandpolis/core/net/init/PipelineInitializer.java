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
import com.sandpolis.core.instance.Instance;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.handler.CvidHandler;
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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * This class provides a base for other {@link ChannelInitializer}s which can
 * configure a {@link ChannelPipeline} for use as a specific connection (server,
 * client, or viewer).
 * 
 * @see ServerInitializer
 * @see ViewerInitializer
 * @see ClientInitializer
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class PipelineInitializer extends ChannelInitializer<Channel> {

	private static final Logger log = LoggerFactory.getLogger(PipelineInitializer.class);

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
	 * The global CVID handler.
	 */
	protected static final CvidHandler CVID = new CvidHandler();

	/**
	 * The local protocol buffer frame decoder.
	 */
	protected final ProtobufVarint32FrameDecoder PROTO_FRAME_DECODER = new ProtobufVarint32FrameDecoder();

	/**
	 * The local traffic limiter.
	 */
	protected ChannelTrafficShapingHandler traffic = new ChannelTrafficShapingHandler(4000);

	/**
	 * The {@link Exelet}s to initialize a {@link ExecuteHandler} with.
	 */
	private Class<? extends Exelet>[] exelets;

	public PipelineInitializer(Class<? extends Exelet>[] exelets, Instance instance) {
		if (exelets == null)
			throw new IllegalArgumentException();

		this.exelets = exelets;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		log.debug("Initializing new channel");
		ChannelPipeline p = ch.pipeline();

		p.addLast("traffic", traffic);
		ch.attr(Sock.TRAFFIC_HANDLER_KEY).set(traffic);

		SslContext sslContext = getSslContext();
		if (sslContext != null) {
			SslHandler ssl = sslContext.newHandler(ch.alloc());
			p.addLast("ssl", ssl);
			ch.attr(Sock.SSL_HANDLER_KEY).set(ssl);
		}

		if (Core.LOG_NET_RAW)
			p.addLast(LOGGING);

		p.addLast(EVENT);

		p.addLast(PROTO_FRAME_DECODER);
		p.addLast(PROTO_DECODER);
		p.addLast(PROTO_FRAME_ENCODER);
		p.addLast(PROTO_ENCODER);

		if (Core.LOG_NET)
			p.addLast(LOGGING);

		p.addLast(CVID);

		// TODO add EventExecutorGroup!
		ExecuteHandler execute = new ExecuteHandler(exelets);
		p.addLast("exe", execute);
		ch.attr(Sock.EXECUTE_HANDLER_KEY).set(execute);

	}

	/**
	 * Get an {@link SslContext} for the initializer.
	 * 
	 * @return An optionally new {@link SslContext} or {@code null}
	 * @throws Exception
	 */
	public abstract SslContext getSslContext() throws Exception;

}
