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

/// Contains client metadata
class SandpolisProfile {

	/// The client's UUID
	var uuid: String

	/// The client's CVID
	var cvid: Int32

	/// The client's IP address
	var ipAddress: String

	/// The client's OS type
	var platform: Util_OsType
	
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

	/// The client's timezone
	var timezone: TimeZone?

	/// The client's latitude
	var latitude: Double?

	/// The client's longitude
	var longitude: Double?

	/// The client's country name
	var country: String?

	/// The client's country code
	var countryCode: String?

	/// The client's region name
	var region: String?

	/// The client's city name
	var city: String?

	/// Whether the client is online
	var online: Bool

	init(uuid: String, cvid: Int32, hostname: String, ipAddress: String, platform: Util_OsType, online: Bool) {
		self.uuid = uuid
		self.cvid = cvid
		self.hostname = hostname
		self.ipAddress = ipAddress
		self.platform = platform
		self.online = online
	}
}
