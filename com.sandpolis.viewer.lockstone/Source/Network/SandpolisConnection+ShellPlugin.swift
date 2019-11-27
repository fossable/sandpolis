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
import SwiftProtobuf
import os

extension SandpolisConnection {
	
	/// Request a new remote desktop session from the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Parameter receiver: The controller to receive events
	/// - Returns: The stream
	func shell_session(_ target: Int32, _ receiver: ShellSession, _ shell: Net_Shell) -> SandpolisStream {
		let stream = SandpolisStream(self, SandpolisUtil.stream())
		stream.register { (m: Net_MSG) -> Void in
			receiver.onEvent(try! Net_ShellMSG.init(unpackingAny: m.plugin).evShellStream)
		}
		streams.append(stream)

		var rq = Net_MSG.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
				$0.rqShellStream = Net_RQ_ShellStream.with {
					$0.id = stream.id
					$0.type = shell
				}
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
		var rq = Net_MSG.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
				$0.rqListShells = Net_RQ_ListShells()
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting shell list for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Net_ShellMSG.init(unpackingAny: rs.plugin)
			} catch {
				return rs.rsOutcome
			}
		}
	}

	/// Request to shutdown the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func poweroff(_ target: Int32) -> EventLoopFuture<Any> {
		var rq = Net_MSG.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
				$0.rqPowerChange = Net_RQ_PowerChange.with {
					$0.change = .poweroff
				}
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting poweroff for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Net_ShellMSG.init(unpackingAny: rs.plugin)
			} catch {
				return rs.rsOutcome
			}
		}
	}

	/// Request to restart the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Returns: A response future
	func restart(_ target: Int32) -> EventLoopFuture<Any> {
		var rq = Net_MSG.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
				$0.rqPowerChange = Net_RQ_PowerChange.with {
					$0.change = .restart
				}
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting restart for client: %d", target)
		return request(&rq).map { rs in
			do {
				return try Net_ShellMSG.init(unpackingAny: rs.plugin)
			} catch {
				return rs.rsOutcome
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
	func execute(_ target: Int32, _ shell: Net_Shell, _ script: String, _ timeout: TimeInterval = 0) -> EventLoopFuture<Any> {
		var rq = Net_MSG.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
				$0.rqExecute = Net_RQ_Execute.with {
					$0.type = shell
					$0.command = script
				}
			}, typePrefix: "com.sandpolis.plugin.shell")
		}

		os_log("Requesting macro execution for client: %d", target)
		return request(&rq, timeout).map { rs in
			do {
				return try Net_ShellMSG.init(unpackingAny: rs.plugin)
			} catch {
				return rs.rsOutcome
			}
		}
	}
}
