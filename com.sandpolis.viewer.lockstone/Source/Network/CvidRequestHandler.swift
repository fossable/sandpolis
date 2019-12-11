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
import NIO
import Foundation

/// Handles the CVID handshake
final class CvidRequestHandler: ChannelInboundHandler, RemovableChannelHandler {
	typealias InboundIn = Net_MSG

	private let connection: SandpolisConnection

	init(_ connection: SandpolisConnection) {
		self.connection = connection
	}

	func channelRead(context: ChannelHandlerContext, data: NIOAny) {
		// Autoremove
		_ = context.channel.pipeline.removeHandler(self)

		let rs = self.unwrapInboundIn(data).rsCvid
		connection.cvid = rs.cvid
		connection.handshakeCompleted = true
		connection.connectionPromise.succeed(Void())
	}

	func channelActive(context: ChannelHandlerContext) {
		let rq = Net_MSG.with({
			$0.rqCvid = Net_RQ_Cvid.with({
				$0.instance = Util_Instance.viewer
				$0.instanceFlavor = Util_InstanceFlavor.lockstone
				$0.uuid = UserDefaults.standard.string(forKey: "uuid")!
			})
		})

		_ = context.channel.writeAndFlush(rq)
	}
}
