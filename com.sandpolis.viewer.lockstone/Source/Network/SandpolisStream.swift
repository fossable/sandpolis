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

/// Represents the endpoint of a server stream
class SandpolisStream {

	private let connection: SandpolisConnection

	let id: Int32

	private var listeners = [(Net_MSG) -> Void]()

	init(_ connection: SandpolisConnection, _ id: Int32) {
		self.connection = connection
		self.id = id
	}

	func consume(_ data: Net_MSG) {
		for listener in listeners {
			listener(data)
		}
	}

	func register(_ handler: @escaping (Net_MSG) -> Void) {
		listeners.append(handler)
	}

	/// Close the stream
	func close() -> EventLoopFuture<Net_MSG> {
		var rq = Net_MSG.with {
			$0.rqStreamStop = Net_RQ_StreamStop.with {
				$0.id = id
			}
		}

		return connection.request(&rq)
	}
}
