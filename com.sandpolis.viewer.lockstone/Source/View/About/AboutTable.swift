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
import UIKit

class VersionTable: UITableViewController {
	private let bundles = [
		["name": "Highlightr", "id": "org.cocoapods.Highlightr"],
		["name": "CryptoSwift", "id": "org.cocoapods.CryptoSwift"],
		["name": "SwiftKeychainWrapper", "id": "org.cocoapods.SwiftKeychainWrapper"],
		["name": "SwiftProtobuf", "id": "org.cocoapods.Protobuf"],
		["name": "GoogleUtilities", "id": "org.cocoapods.GoogleUtilities"]
	]

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return bundles.count
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let cell = tableView.dequeueReusableCell(withIdentifier: "VersionCell") as! VersionCell
		cell.setContent(bundles[indexPath.row])
		return cell
	}
}
