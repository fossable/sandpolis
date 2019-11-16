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

class GroupActions: UITableViewController {

	var profiles = [SandpolisProfile]()

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "MacroSelectSegue",
			let macroSelect = segue.destination as? MacroSelect {
			macroSelect.profiles = profiles
		}
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		tableView.deselectRow(at: indexPath, animated: true)

		if indexPath.row == 0 {
			let alert = UIAlertController(title: "Are you sure?", message: "\(profiles.count) hosts will be powered off immediately", preferredStyle: .alert)
			alert.addAction(UIAlertAction(title: "Poweroff", style: .destructive) { _ in
					for profile in self.profiles {
						_ = SandpolisUtil.connection.poweroff(profile.cvid)
					}
					self.navigationController?.popViewController(animated: true)
				})
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

			present(alert, animated: true)
		} else if indexPath.row == 1 {
			let alert = UIAlertController(title: "Are you sure?", message: "\(profiles.count) hosts will be restarted immediately", preferredStyle: .alert)
			alert.addAction(UIAlertAction(title: "Restart", style: .destructive) { _ in
					for profile in self.profiles {
						_ = SandpolisUtil.connection.restart(profile.cvid)
					}
					self.navigationController?.popViewController(animated: true)
				})
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

			present(alert, animated: true)
		} else if indexPath.row == 3 {
			let alert = UIAlertController(title: "Create New Host Group", message: "Enter a group name.", preferredStyle: .alert)
			alert.addTextField { (textField) in
				textField.text = ""
			}
			// TODO
			self.present(alert, animated: true, completion: nil)
		}
	}
}
