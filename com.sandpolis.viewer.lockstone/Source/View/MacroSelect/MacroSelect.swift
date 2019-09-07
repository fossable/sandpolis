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
import FirebaseAuth
import FirebaseFirestore

class MacroSelect: UITableViewController {

	/// Firebase reference
	private let ref = Firestore.firestore().collection("/user/\(Auth.auth().currentUser!.uid)/macro")

	var profiles = [SandpolisProfile]()

	var macroList = [DocumentSnapshot]()

	override func viewDidLoad() {
		super.viewDidLoad()

		// Synchronize list with Firebase
		ref.getDocuments { querySnapshot, error in
			guard let macros = querySnapshot?.documents else {
				return
			}

			self.macroList = macros.filter { macro in
				// Ensure macro is compatible with every profile
				for profile in self.profiles {
					switch profile.platform {
					case .linux:
						if !(macro["linux"] as! Bool) {
							return false
						}
					case .windows:
						if !(macro["windows"] as! Bool) {
							return false
						}
					case .macos:
						if !(macro["macos"] as! Bool) {
							return false
						}
					default:
						print("Warning: Unknown platform")
					}
				}
				return true
			}
			self.tableView.reloadData()
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "MacroExecuteSegue",
			let resultView = segue.destination as? MacroResults {

			resultView.profiles = profiles
			resultView.macro = macroList[tableView.indexPathForSelectedRow!.row]
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		if macroList.count == 0 {
			let message = UILabel(frame: tableView.bounds)
			message.textAlignment = .center
			message.sizeToFit()
			message.text = "No compatible macros found!"
			message.isEnabled = false
			tableView.backgroundView = message
		} else {
			tableView.backgroundView = nil
		}
		return macroList.count
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let cell = tableView.dequeueReusableCell(withIdentifier: "MacroSelectCell") as! MacroSelectCell
		cell.setContent(macroList[indexPath.row])
		return cell
	}

}
