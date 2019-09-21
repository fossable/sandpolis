import NIO
import Foundation
import os

/// A handler for non-request messages
final class ClientHandler: ChannelInboundHandler {
	typealias InboundIn = Net_Message
	
	private let connection: SandpolisConnection

	init(_ connection: SandpolisConnection) {
		self.connection = connection
	}

	func channelRead(context: ChannelHandlerContext, data: NIOAny) {
		let rs = self.unwrapInboundIn(data)

		if rs.msgOneof == nil {
			return
		}

		switch rs.msgOneof! {
		case .evStreamData:
			DispatchQueue.global(qos: .default).async {
				// Linear search to find stream
				if let stream = self.connection.streams.first(where: { $0.id == rs.evStreamData.id }) {
					stream.consume(rs.evStreamData)
				} else {
					// Wait one second and try again before dropping
					sleep(1)
					if let stream = self.connection.streams.first(where: { $0.id == rs.evStreamData.id }) {
						stream.consume(rs.evStreamData)
					} else {
						os_log("Warning: dropped data for stream: %s", rs.evStreamData.id)
					}
				}
			}
		default:
			os_log("Missing handler for message")
		}
	}
}
