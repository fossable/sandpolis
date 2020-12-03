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
import SwiftProtobuf
import Foundation

/// Handles the CVID handshake
final class CvidRequestHandler: ChannelInboundHandler, RemovableChannelHandler {
	typealias InboundIn = Core_Net_MSG

	private let connection: SandpolisConnection

	init(_ connection: SandpolisConnection) {
		self.connection = connection
	}

	func channelRead(context: ChannelHandlerContext, data: NIOAny) {
		// Autoremove
		_ = context.channel.pipeline.removeHandler(self)

		let rs = try! Core_Net_Msg_RS_Cvid.init(serializedData: self.unwrapInboundIn(data).payload)
		connection.cvid = rs.cvid
		connection.handshakeCompleted = true
		connection.connectionPromise.succeed(Void())
	}

	func channelActive(context: ChannelHandlerContext) {
		let rq = Core_Net_MSG.with {
			$0.payload = try! Core_Net_Msg_RQ_Cvid.with {
				$0.instance = Core_Instance_InstanceType.client
				$0.instanceFlavor = Core_Instance_InstanceFlavor.lockstone
				$0.uuid = UserDefaults.standard.string(forKey: "uuid")!
            }.serializedData()
		}

		_ = context.channel.writeAndFlush(rq)
	}
}
