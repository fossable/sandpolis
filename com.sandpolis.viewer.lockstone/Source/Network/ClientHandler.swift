//****************************************************************************//
//                                                                            //
//                Copyright © 2015 - 2019 Subterranean Security               //
//                                                                            //
//  Licensed under the Apache License, Version 2.0 (the "License");           //
//  you may not use this file except in compliance with the License.          //
//  You may obtain a copy of the License at                                   //
//                                                                            //
//      http://www.apache.org/licenses/LICENSE-2.0                            //
//                                                                            //
//  Unless required by applicable law or agreed to in writing, software       //
//  distributed under the License is distributed on an "AS IS" BASIS,         //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  //
//  See the License for the specific language governing permissions and       //
//  limitations under the License.                                            //
//                                                                            //
//****************************************************************************//
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
