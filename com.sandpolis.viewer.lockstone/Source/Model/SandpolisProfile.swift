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
import UIKit
import os

/// Contains client metadata
class SandpolisProfile {

	/// The client's UUID
	let uuid: String

	/// The client's CVID
	let cvid: Int32
	
	/// The client's OS type
	let platform: Util_OsType
	
	/// Whether the client is currently online
	var online: Bool

	/// The client's IP address
	var ipAddress: String?

	/// The client's hostname
	var hostname: String?

	/// The saved desktop screenshot
	var screenshot: Data?

	/// The client's OS version
	var osVersion: String?

	/// The user's username
	var username: String?

	/// The user's home directory
	var userhome: String?

	/// The client's start timestamp
	var startTime: Int64?

	/// The client's IP location
	var location: Util_Location?
	
	/// A list of shells that the client supports
	lazy var shells: [Net_RS_ListShells.ShellListing]? = {
		do {
			if let rs = try SandpolisUtil.connection.shell_list(cvid).wait() as? Net_ShellMSG {
				return rs.rsListShells.listing
			}
		} catch {
			os_log("Failed to query shells")
		}
		return nil
	}()

	init(uuid: String, cvid: Int32, hostname: String, ipAddress: String, platform: Util_OsType, online: Bool) {
		self.uuid = uuid
		self.cvid = cvid
		self.hostname = hostname
		self.ipAddress = ipAddress
		self.platform = platform
		self.online = online
	}
	
	var platformIcon: UIImage? {
		switch platform {
		case .windows:
			return UIImage(named: "platform/windows")
		case .linux:
			return UIImage(named: "platform/linux")
		case .macos:
			return UIImage(named: "platform/mac")
		case .freebsd:
			return UIImage(named: "platform/freebsd")
		default:
			return nil
		}
	}
	
	var platformGlyph: UIImage? {
		switch platform {
		case .windows:
			return UIImage(named: "platform/windows-glyph")
		case .linux:
			return UIImage(named: "platform/linux")
		case .macos:
			return UIImage(named: "platform/mac-glyph")
		case .freebsd:
			return UIImage(named: "platform/freebsd-glyph")
		default:
			return nil
		}
	}
}
