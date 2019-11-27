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
