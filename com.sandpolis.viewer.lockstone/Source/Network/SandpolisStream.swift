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
	let id: Int32

	private var listeners = [(Net_EV_StreamData) -> Void]()

	init(_ id: Int32) {
		self.id = id
	}

	func consume(_ data: Net_EV_StreamData) {
		for listener in listeners {
			listener(data)
		}
	}

	func register(_ handler: @escaping (Net_EV_StreamData) -> Void) {
		listeners.append(handler)
	}

	/// Close the stream
	func close() -> EventLoopFuture<Net_Message> {
		var rq = Net_Message.with {
			$0.rqStreamStop = Net_RQ_StreamStop.with {
				$0.streamID = id
			}
		}

		return SandpolisUtil.request(&rq)
	}
}
