//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import NIO
import Foundation
import os

/// A handler for non-request messages
final class ClientHandler: ChannelInboundHandler {
	typealias InboundIn = Core_Net_MSG

	private let connection: SandpolisConnection

	init(_ connection: SandpolisConnection) {
		self.connection = connection
	}

	func channelRead(context: ChannelHandlerContext, data: NIOAny) {
		let rs = self.unwrapInboundIn(data)

		if let stream = self.connection.streams.first(where: { $0.id == rs.id }) {
			stream.consume(rs)
		}
	}
}
