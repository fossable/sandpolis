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
import Foundation
import NIO

/// A handler for request responses
final class ResponseHandler: ChannelInboundHandler {
	typealias InboundIn = Net_Message

	/// A map of response IDs to response future
	private var responseMap: [Int32: EventLoopPromise<Net_Message>] = [:]

	/// An internal queue for synchronization
	private let queue = DispatchQueue(label: "ResponseHandlerInternal")

	func channelRead(context: ChannelHandlerContext, data: NIOAny) {
		let rs = self.unwrapInboundIn(data)

		if queue.sync(execute: {
			if let p = responseMap.removeValue(forKey: rs.id) {
				// This is a response
				p.succeed(rs)
				return false
			}
			return true
		}) {
			// Pass down pipeline
			context.fireChannelRead(data)
		}
	}
	
	/// Register the given promise to receive a response message.
	///
	/// - Parameter id: The message id
	/// - Parameter promise: The response promise
	public func register(_ id: Int32, _ promise: EventLoopPromise<Net_Message>) {
		queue.sync {
			responseMap[id] = promise
		}
	}
}
