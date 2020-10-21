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

	/// Request a new remote desktop session from the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Parameter receiver: The controller to receive events
	/// - Parameter shell: The shell type
	/// - Parameter cols: The initial column setting
	/// - Parameter rows: The initial rows setting
	/// - Returns: The stream
	func shell_session(_ target: Int32, _ receiver: ShellSession, _ shell: Plugin_Shell_Msg_Shell, _ cols: Int32, _ rows: Int32) -> SandpolisStream {
		let stream = SandpolisStream(self, SandpolisUtil.stream())
		stream.register { (m: Core_Net_MSG) -> Void in
			if let ev = try? Plugin_Shell_Msg_EV_ShellStream.init(unpackingAny: m.payload) {
				receiver.onEvent(ev)
			}
		}
		streams.append(stream)

		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Google_Protobuf_Any(message: Plugin_Shell_Msg_RQ_ShellStream.with {
				$0.id = stream.id
				$0.type = shell
				$0.cols = cols
				$0.rows = rows
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting remote shell session for client: %d", target)
		_ = request(&rq)
		return stream
	}

	/// Request the compatible shells on the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func shell_list(_ target: Int32) -> EventLoopFuture<Any> {
		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Google_Protobuf_Any(message: Plugin_Shell_Msg_RQ_ListShells(), typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting shell list for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Plugin_Shell_Msg_RS_ListShells.init(unpackingAny: rs.payload)
			} catch {
				do {
					return try Core_Foundation_Outcome.init(unpackingAny: rs.payload)
				} catch {
					return Core_Foundation_Outcome.with {
						$0.result = false
					}
				}
			}
		}
	}

	/// Request to shutdown the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func poweroff(_ target: Int32) -> EventLoopFuture<Any> {
		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Google_Protobuf_Any(message: Plugin_Shell_Msg_RQ_PowerChange.with {
				$0.change = .poweroff
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting poweroff for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Core_Foundation_Outcome.init(unpackingAny: rs.payload)
			} catch {
				return Core_Foundation_Outcome.with {
					$0.result = false
				}
			}
		}
	}

	/// Request to restart the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func restart(_ target: Int32) -> EventLoopFuture<Any> {
		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Google_Protobuf_Any(message: Plugin_Shell_Msg_RQ_PowerChange.with {
				$0.change = .restart
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting restart for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Core_Foundation_Outcome.init(unpackingAny: rs.payload)
			} catch {
				return Core_Foundation_Outcome.with {
					$0.result = false
				}
			}
		}
	}

	/// Request to execute the script on the given client.
	///
	/// - Parameter cvid: The target client's CVID
	/// - Parameter shell: The target shell type
	/// - Parameter script: The macro script
	/// - Parameter timeout: The execution timeout in seconds
	/// - Returns: A response future
	func execute(_ target: Int32, _ shell: Plugin_Shell_Msg_Shell, _ script: String, _ timeout: TimeInterval = 0) -> EventLoopFuture<Any> {
		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Google_Protobuf_Any(message: Plugin_Shell_Msg_RQ_Execute.with {
				$0.type = shell
				$0.command = script
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting macro execution for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Core_Foundation_Outcome.init(unpackingAny: rs.payload)
			} catch {
				return Core_Foundation_Outcome.with {
					$0.result = false
				}
			}
		}
	}
}
