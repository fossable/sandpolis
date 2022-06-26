//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import SwiftKeychainWrapper
import Foundation

/// Represents a Sandpolis server
class SandpolisServer {

	/// The server name
	var name: String

	/// The server address
	var address: String

	/// The client username for the server
	var username: String

	/// Whether the server is online
	var online: Bool?

	/// The server's country code
	var countryCode: String?

	init?(_ data: String) {
		guard let dictionary = try? JSONSerialization.jsonObject(with: data.data(using: .utf8)!, options: []) as? [String: Any] else {
			return nil
		}

		if let name = dictionary["name"] as? String {
			self.name = name
		} else {
			return nil
		}

		if let address = dictionary["address"] as? String {
			self.address = address
		} else {
			return nil
		}

		if let username = dictionary["username"] as? String {
			self.username = username
		} else {
			return nil
		}

		self.countryCode = ""
	}

	func getPassword() -> String? {
		return KeychainWrapper.standard.string(forKey: "server.\(address).password")
	}
}
