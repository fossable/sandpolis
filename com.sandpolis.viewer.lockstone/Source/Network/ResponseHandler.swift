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
import Foundation
import NIO

/// A handler for request responses
final class ResponseHandler: ChannelInboundHandler {
	typealias InboundIn = Net_MSG

	/// A map of response IDs
	private var responseMap: [Int32: (EventLoopPromise<Net_MSG>, Timer?)] = [:]

	/// An internal queue for synchronization
	private let queue = DispatchQueue(label: "ResponseHandlerInternal")

	func channelRead(context: ChannelHandlerContext, data: NIOAny) {
		let rs = self.unwrapInboundIn(data)

		if queue.sync(execute: {
			if let p = responseMap.removeValue(forKey: rs.id) {
				// This is a response
				if let timer = p.1 {
					timer.invalidate()
				}
				p.0.succeed(rs)
				return false
			}
			return true
		}) {
			// Pass down the pipeline
			context.fireChannelRead(data)
		}
	}

	/// Register the given promise to receive a response message.
	///
	/// - Parameter id: The message id
	/// - Parameter promise: The response promise
	/// - Parameter timeout: The timeout
	public func register(_ id: Int32, _ promise: EventLoopPromise<Net_MSG>, _ timeout: TimeInterval) {
		queue.sync {
			if let p = self.responseMap.removeValue(forKey: id) {
				// Cancel timer before overwriting
				if let timer = p.1 {
					timer.invalidate()
				}
				p.0.fail(ResponseError.timeout)
			}

			if timeout > 0 {
				responseMap[id] = (promise, Timer.scheduledTimer(withTimeInterval: timeout, repeats: false) { _ in
					self.queue.sync {
						if let p = self.responseMap.removeValue(forKey: id) {
							p.0.fail(ResponseError.timeout)
						}
					}
				})
			} else {
				responseMap[id] = (promise, nil)
			}
		}
	}

	enum ResponseError: Error {
		case timeout
	}
}
