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
	
	/// Request a file listing from the given client.
	///
	/// - Parameters:
	///   - target: The target client's CVID
	///   - path: The target path
	///   - mtimes: Whether modification times will be returned
	///   - sizes: Whether file sizes will be returned
	/// - Returns: A response future
	func fm_list(_ target: Int32, _ path: String, mtimes: Bool, sizes: Bool) -> EventLoopFuture<Any> {
		var rq = Net_MSG.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_FilesysMSG.with {
				$0.rqFileListing = Net_RQ_FileListing.with {
					$0.path = path
					$0.options = Net_FsHandleOptions.with {
						$0.mtime = mtimes
						$0.size = sizes
					}
				}
			}, typePrefix: "com.sandpolis.plugin.filesys")
		}

		os_log("Requesting file listing for client: %d, path: %s", target, path)
		return request(&rq).map { rs in
			do {
				return try Net_FilesysMSG.init(unpackingAny: rs.plugin)
			} catch {
				return rs.rsOutcome
			}
		}
	}

	/// Delete a file on the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Parameter path: The target file
	/// - Returns: A response future
	func fm_delete(_ target: Int32, _ path: String) -> EventLoopFuture<Any> {
		var rq = Net_MSG.with {
			$0.to = target
			$0.plugin = try! Google_Protobuf_Any(message: Net_FilesysMSG.with {
				$0.rqFileDelete = Net_RQ_FileDelete.with {
					$0.target = [path]
				}
			}, typePrefix: "com.sandpolis.plugin.filesys")
		}

		os_log("Requesting file deletion for client: %d, path: %s", target, path)
		return request(&rq).map { rs in
			do {
				return try Net_FilesysMSG.init(unpackingAny: rs.plugin)
			} catch {
				return rs.rsOutcome
			}
		}
	}
}
