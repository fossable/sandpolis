//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import Foundation
import NIO
import NIOTLS

/// A handler for lifecycle events
final class ManagementHandler: ChannelInboundHandler {
	typealias InboundIn = Any

	private let connection: SandpolisConnection

	init(_ connection: SandpolisConnection) {
		self.connection = connection
	}

	func errorCaught(context: ChannelHandlerContext, error: Error) {
		if !connection.handshakeCompleted {
			connection.connectionPromise.fail(error)
		}
	}

	func channelInactive(context: ChannelHandlerContext) {
		connection.disconnect()
	}
}
