/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
import NIO

/// Handles the CVID handshake and removes itself from the pipeline on completion
final class CvidRequestHandler: ChannelInboundHandler, RemovableChannelHandler {
	typealias InboundIn = Net_Message

	var handshakePromise: EventLoopPromise<Int32>

	init(_ eventLoop: EventLoop) {
		self.handshakePromise = eventLoop.makePromise()
	}

	func channelRead(context: ChannelHandlerContext, data: NIOAny) {
		// Autoremove
		_ = context.channel.pipeline.removeHandler(self)

		let rs = self.unwrapInboundIn(data).rsCvid
		SandpolisUtil.cvid = rs.cvid
		handshakePromise.succeed(rs.cvid)
	}

	func channelActive(context: ChannelHandlerContext) {
		let rq = Net_Message.with({
			$0.rqCvid = Net_RQ_Cvid.with({
				$0.instance = Util_Instance.viewer
				$0.instanceFlavor = Util_InstanceFlavor.lockstone
				$0.uuid = SandpolisUtil.uuid
			})
		})

		_ = context.channel.writeAndFlush(rq)
	}
}
