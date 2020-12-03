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

/// Represents the endpoint of a server stream
class SandpolisStream {

	let connection: SandpolisConnection

	let id: Int32

	private var listeners = [(Core_Net_MSG) -> Void]()

	init(_ connection: SandpolisConnection, _ id: Int32) {
		self.connection = connection
		self.id = id
	}

	func consume(_ data: Core_Net_MSG) {
		for listener in listeners {
			listener(data)
		}
	}

	func register(_ handler: @escaping (Core_Net_MSG) -> Void) {
		listeners.append(handler)
	}

	/// Close the stream
	func close() -> EventLoopFuture<Core_Net_MSG> {
		var rq = Core_Net_MSG.with {
			$0.payload = try! Core_Net_Msg_RQ_StreamStop.with {
				$0.id = id
            }.serializedData()
		}

		return connection.request(&rq)
	}
}
