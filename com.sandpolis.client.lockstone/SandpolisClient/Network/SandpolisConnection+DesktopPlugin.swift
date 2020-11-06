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
import SwiftProtobuf
import os

extension SandpolisConnection {

	/// Request a screenshot from the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func screenshot(_ target: Int32) -> EventLoopFuture<Any> {
		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Google_Protobuf_Any(message: Plugin_Desktop_Msg_RQ_Screenshot(), typePrefix: "com.sandpolis.plugin.desktop")
		}

		os_log("Requesting screenshot for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Plugin_Desktop_Msg_RS_Screenshot.init(unpackingAny: rs.payload)
			} catch {
				return Core_Foundation_Outcome.with {
					$0.result = false
				}
			}
		}
	}

	/// Request a new remote desktop session from the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Parameter receiver: The controller to receive events
	/// - Returns: The stream
	func remote_desktop(_ target: Int32, _ receiver: RemoteDesktop) -> SandpolisStream {
		let stream = SandpolisStream(self, SandpolisUtil.stream())
		stream.register { (ev: Core_Net_MSG) -> Void in
			do {
				receiver.onEvent(try Plugin_Desktop_Msg_EV_DesktopStream.init(unpackingAny: ev.payload))
			} catch {
				os_log("Failed to decode stream event")
			}
		}
		streams.append(stream)

		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Google_Protobuf_Any(message:  Plugin_Desktop_Msg_RQ_DesktopStream.with {
				$0.id = stream.id
			}, typePrefix: "com.sandpolis.plugin.desktop")
		}

		os_log("Requesting remote desktop session for client: %d", target)
		_ = request(&rq)
		return stream
	}
}
