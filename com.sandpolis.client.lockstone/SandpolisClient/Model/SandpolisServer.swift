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
import FirebaseFirestore

/// Represents a Sandpolis server
class SandpolisServer {

	var reference: DocumentReference

	/// The server name
	var name: String

	/// The server address
	var address: String

	/// The client username for the server
	var username: String

	/// The client password for the server (unsalted SHA256)
	var password: String

	/// Whether the server is a cloud server
	var cloud: Bool

	/// Whether the server is online
	var online: Bool?

	/// The server's country code
	var countryCode: String?

	init(_ server: DocumentSnapshot) {
		self.reference = server.reference
		self.name = server["name"] as! String
		self.address = server["address"] as! String
		self.username = server["username"] as! String
		self.password = server["password"] as! String
		self.cloud = server["cloud"] as! Bool
	}
}
