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

	/// Request a file listing from the given client.
	///
	/// - Parameters:
	///   - target: The target client's CVID
	///   - path: The target path
	///   - mtimes: Whether modification times will be returned
	///   - sizes: Whether file sizes will be returned
	/// - Returns: A response future
	func fm_list(_ target: Int32, _ path: String, mtimes: Bool, sizes: Bool) -> EventLoopFuture<Any> {
		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Plugin_Filesys_Msg_RQ_FileListing.with {
				$0.path = path
				$0.options = Plugin_Filesys_Msg_FsHandleOptions.with {
					$0.mtime = mtimes
					$0.size = sizes
				}
            }.serializedData()
		}

		os_log("Requesting file listing for client: %d, path: %s", target, path)
		return request(&rq).map { rs in
			do {
				return try Plugin_Filesys_Msg_RS_FileListing.init(serializedData: rs.payload)
			} catch {
				return try! Core_Foundation_Outcome.init(serializedData: rs.payload)
			}
		}
	}

	/// Delete a file on the given client.
	///
	/// - Parameter target: The target client's CVID
	/// - Parameter path: The target file
	/// - Returns: A response future
	func fm_delete(_ target: Int32, _ path: String) -> EventLoopFuture<Any> {
		var rq = Core_Net_MSG.with {
			$0.to = target
			$0.payload = try! Plugin_Filesys_Msg_RQ_FileDelete.with {
				$0.target = [path]
            }.serializedData()
		}

		os_log("Requesting file deletion for client: %d, path: %s", target, path)
		return request(&rq).map { rs in
			do {
				return try Core_Foundation_Outcome.init(serializedData: rs.payload)
			} catch {
				// TODO
				return try! Core_Foundation_Outcome.init(serializedData: rs.payload)
			}
		}
	}
}
