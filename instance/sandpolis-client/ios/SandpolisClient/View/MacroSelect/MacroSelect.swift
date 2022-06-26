//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class MacroSelect: UITableViewController {

	/// Firebase reference
	//private let ref = Firestore.firestore().collection("/user/\(Auth.auth().currentUser!.uid)/macro")

	var profiles = [SandpolisProfile]()

	//var macroList = [DocumentSnapshot]()

	override func viewDidLoad() {
		super.viewDidLoad()
		navigationItem.title = "\(profiles.count) clients selected"

		// Synchronize list with Firebase
		/*ref.getDocuments { querySnapshot, error in
			guard let macros = querySnapshot?.documents else {
				return
			}

			self.macroList = macros.filter { macro in

				let type: Plugin_Shell_Msg_Shell
				switch macro["type"] as! String {
				case "powershell":
					type = .pwsh
				case "cmd":
					type = .cmd
				case "bash":
					type = .bash
				case "zsh":
					type = .zsh
				default:
					type = .bash
				}

				// Ensure macro is compatible with every profile
				for profile in self.profiles {
					if let shells = profile.shells {
						for shell in shells {
							if shell.type == type {
								return true
							}
						}
					}
					return false
				}
				return false
			}
			self.tableView.reloadData()
		}*/
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "MacroExecuteSegue",
			let resultView = segue.destination as? MacroResults {

			resultView.profiles = profiles
			//resultView.macro = macroList[tableView.indexPathForSelectedRow!.row]
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	/*override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
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
	}*/

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let cell = tableView.dequeueReusableCell(withIdentifier: "MacroSelectCell") as! MacroSelectCell
		//cell.setContent(macroList[indexPath.row])
		return cell
	}

}
