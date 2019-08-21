/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
import UIKit

class GroupPanel: UIViewController, UITableViewDelegate, UITableViewDataSource {

	@IBOutlet weak var table: UITableView!
	var hostList: ClientList!
	var profiles = [SandpolisProfile]()

	override func viewDidLoad() {
		super.viewDidLoad()
		table.delegate = self
		table.dataSource = self
		navigationItem.title = "\(profiles.count) hosts selected"
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "StatsTable",
			let table = segue.destination as? GroupPanelTable {
			table.profiles = profiles
		} else if segue.identifier == "MacroSegue",
			let resultsView = segue.destination as? MacroSelect {
			resultsView.profiles = profiles
		} else if segue.identifier == "SingleControlPanelSegue",
			let cpView = segue.destination as? ControlPanel {
			cpView.profile = profiles[table.indexPathForSelectedRow!.row]
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return profiles.count
	}

	func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
		return "Hosts"
	}

	func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let cell = tableView.dequeueReusableCell(withIdentifier: "GroupHostCell", for: indexPath) as! GroupHostCell
		cell.setContent(profiles[indexPath.row])
		return cell
	}

	@IBAction func poweroff(_ sender: Any) {
		let alert = UIAlertController(title: "Are you sure?", message: "\(profiles.count) hosts will be powered off immediately", preferredStyle: .alert)
		alert.addAction(UIAlertAction(title: "Poweroff", style: .destructive) { _ in
				for profile in self.profiles {
					SandpolisUtil.poweroff(profile.cvid)
				}
				self.navigationController?.popViewController(animated: true)
			})
		alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

		present(alert, animated: true)
	}

	@IBAction func reboot(_ sender: Any) {
		let alert = UIAlertController(title: "Are you sure?", message: "\(profiles.count) hosts will be restarted immediately", preferredStyle: .alert)
		alert.addAction(UIAlertAction(title: "Restart", style: .destructive) { _ in
				for profile in self.profiles {
					SandpolisUtil.restart(profile.cvid)
				}
				self.navigationController?.popViewController(animated: true)
			})
		alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

		present(alert, animated: true)
	}

	@IBAction func createGroupButtonPressed(_ sender: Any) {
		let alert = UIAlertController(title: "Create New Host Group", message: "Enter a group name.", preferredStyle: .alert)
		alert.addTextField { (textField) in
			textField.text = ""
		}
		alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [weak alert] (_) in
			let textField = alert?.textFields![0]
			let error = self.hostList.addHostGroup(groupName: textField!.text!, profiles: self.profiles)
			var responseAlert: UIAlertController!
			if error == "" {
				responseAlert = UIAlertController(title: "Success", message: "Host group created successfully.", preferredStyle: .alert)
			} else {
				responseAlert = UIAlertController(title: "Error", message: error, preferredStyle: .alert)
			}
			responseAlert.addAction(UIAlertAction(title: "OK", style: .default))
			self.present(responseAlert, animated: true, completion: nil)
		}))
		self.present(alert, animated: true, completion: nil)
	}
}
