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
import FirebaseFirestore

class ClientList: UITableViewController {

	@IBOutlet weak var footerView: UIView!

	/// The server for these clients
	var server: SandpolisServer!

	/// All host groups found in firebase (including those from other servers)
	var rawHostGroups = [HostGroup]()

	/// Valid host groups after being filtered by server and online hosts
	var hostGroups = [HostGroup]()

	/// Map from host uuid to the host profile
	var idToProfile: [String: SandpolisProfile] = [:]

	/// Whether multiple hosts can be selected currently
	var multiSelectMode = false

	var selectedIndexPaths = Set<IndexPath>()

	override func viewDidLoad() {
		super.viewDidLoad()
		toggleMultiSelect(toggle: false)
		SandpolisUtil.connection.registerHostUpdates(self.onHostUpdate)
		SandpolisUtil.connection.registerDisconnectHandler(self.onServerDisconnect)

		tableView.addGestureRecognizer(UILongPressGestureRecognizer(target: self, action: #selector(onLongPress)))
	}

	override func viewWillAppear(_ animated: Bool) {
		super.viewWillAppear(animated)
		toggleMultiSelect(toggle: false)
		SandpolisUtil.connection.openProfileStream()
	}

	override func numberOfSections(in tableView: UITableView) -> Int {
		return hostGroups.count
	}

	override func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
		// if there is only the default host group, remove the header
		let defaultGroup = hostGroups[section].identifier == "default"
		if defaultGroup && hostGroups.count == 1 {
			return tableView.dequeueReusableCell(withIdentifier: "HostGroupHeaderBlank")
		}
		let headerCell = tableView.dequeueReusableCell(withIdentifier: "HostGroupHeaderCell") as! HostGroupHeaderCell
		headerCell.setContent(hostList: self, hostGroup: hostGroups[section], defaultGroup: defaultGroup)
		return headerCell
	}

	override func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
		// if there is only the default host group, set height to 0 to hide the header
		let defaultGroup = hostGroups[section].identifier == "default"
		if defaultGroup && hostGroups.count == 1 {
			return 0
		}
		return tableView.sectionHeaderHeight
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		// we always have at least one row to show the 'no hosts' message
		return max(hostGroups[section].hostIds.count, 1)
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let hostGroup = hostGroups[indexPath.section]
		// if there are no hosts in this section, we return the 'no hosts found' cell message
		if hostGroup.hostIds.count == 0 {
			return tableView.dequeueReusableCell(withIdentifier: "HostCellBlank", for: indexPath)
		}
		let hostId = hostGroup.hostIds[indexPath.row]
		let host = idToProfile[hostId]!
		let cell = tableView.dequeueReusableCell(withIdentifier: "HostCell", for: indexPath) as! HostCell
		cell.setContent(host)
		if multiSelectMode {
			if selectedIndexPaths.contains(indexPath) {
				cell.accessoryType = .checkmark
			} else {
				cell.accessoryType = .none
			}
		} else {
			cell.accessoryType = .disclosureIndicator
		}
		return cell
	}

	override func tableView(_ tableView: UITableView, shouldHighlightRowAt indexPath: IndexPath) -> Bool {
		// cells only selectable if there are any hosts to begin with
		return hostGroups[indexPath.section].hostIds.count > 0
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		// check we are not pressing the 'no hosts found' cell
		let hostGroup = hostGroups[indexPath.section]
		if hostGroup.hostIds.count == 0 {
			return
		}
		if multiSelectMode {
			toggleIndexPathSelection(indexPath: indexPath)
		} else {
			performSegue(withIdentifier: "ShowControlPanelSegue", sender: nil)
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "ShowControlPanelSegue",
			let controlPanel = segue.destination as? ControlPanel {
			// get the host profile at the selected indexPath and send it to the control panel
			let indexPath = tableView.indexPathForSelectedRow!
			let hostGroup = hostGroups[indexPath.section]
			let hostId = hostGroup.hostIds[indexPath.row]
			controlPanel.profile = idToProfile[hostId]
		} else if segue.identifier == "ShowGroupControlPanelSegue",
			let groupControlPanel = segue.destination as? GroupControlPanel {
			// get the list of host profiles that are multi-selected and send it to group control panel
			var hostIds = Set<String>()
			var profiles = [SandpolisProfile]()
			for indexPath in selectedIndexPaths {
				let hostGroup = hostGroups[indexPath.section]
				let hostId = hostGroup.hostIds[indexPath.row]
				hostIds.insert(hostId)
			}
			for hostId in hostIds {
				profiles.append(idToProfile[hostId]!)
			}
			groupControlPanel.profiles = profiles
		} else if segue.identifier == "ShowGroupControlPanelFromHeaderSegue",
			let groupControlPanel = segue.destination as? GroupControlPanel {
			// get the list of host profiles under the selected host group and send it to group control panel
			let hostGroup = sender as! HostGroup
			var profiles = [SandpolisProfile]()
			for hostId in hostGroup.hostIds {
				profiles.append(idToProfile[hostId]!)
			}
			groupControlPanel.profiles = profiles
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	@IBAction func cancelButtonPressed(_ sender: Any) {
		toggleMultiSelect(toggle: false)
	}

	func addHostGroup(groupName: String, profiles: [SandpolisProfile]) -> String {
		if groupName.isEmpty {
			return "Host group name cannot be blank."
		}
		let identifier = server.name + "-" + groupName
		for hostGroup in rawHostGroups {
			if hostGroup.identifier == identifier {
				return "Host group with that name already exists."
			}
		}
		var hostIds = [String]()
		for profile in profiles {
			hostIds.append(profile.uuid)
		}
		/*ref.child(identifier).setValue([
			"identifier": identifier,
			"serverName": server.name,
			"groupName": groupName,
			"hostIds": hostIds
			])*/
		return ""
	}

	func viewHostGroup(hostGroup: HostGroup) {
		performSegue(withIdentifier: "ShowGroupControlPanelFromHeaderSegue", sender: hostGroup)
	}

	func deleteHostGroup(hostGroup: HostGroup) {
		//ref.child(hostGroup.identifier).removeValue()
	}

	func onHostUpdate(_ profile: SandpolisProfile) {
		DispatchQueue.main.async {
			self.refreshTableData()
		}
	}

	// refreshes the main data used by the table
	func refreshTableData() {
		idToProfile.removeAll()
		hostGroups.removeAll()
		// create the host uuid to host profile map
		for profile in SandpolisUtil.connection.profiles {
			idToProfile[profile.uuid] = profile
		}
		// filter all host groups found in firebase to only host groups in this server
		for hostGroup in rawHostGroups {
			if hostGroup.serverName == server.name {
				let validHostGroup = createValidHostGroup(rawHostGroup: hostGroup)
				hostGroups.append(validHostGroup)
			}
		}
		// create a default host group containing leftover hosts and place it at the beginning
		let ungroupedHostIds = getUngroupedHostIds(hostGroups: hostGroups)
		let defaultHostGroup = HostGroup(identifier: "default", serverName: "", groupName: "Ungrouped Hosts",
			hostIds: ungroupedHostIds)
		if defaultHostGroup.hostIds.count > 0 || hostGroups.count == 0 {
			hostGroups.insert(defaultHostGroup, at: 0)
		}
		tableView.reloadData()
	}

	// returns the host group with only hosts that are online
	func createValidHostGroup(rawHostGroup: HostGroup) -> HostGroup {
		var validHostIds = [String]()
		for hostId in rawHostGroup.hostIds {
			if idToProfile[hostId] != nil {
				validHostIds.append(hostId)
			}
		}
		return HostGroup(identifier: rawHostGroup.identifier, serverName: rawHostGroup.serverName, groupName: rawHostGroup.groupName, hostIds: validHostIds)
	}

	// get all host ids that are not referenced by any of the given host groups
	func getUngroupedHostIds(hostGroups: [HostGroup]) -> [String] {
		var ungroupedHostIds = Set(idToProfile.keys)
		// remove all host ids that are found in the host groups
		for hostGroup in hostGroups {
			for hostId in hostGroup.hostIds {
				if ungroupedHostIds.contains(hostId) {
					ungroupedHostIds.remove(hostId)
				}
			}
		}
		return Array(ungroupedHostIds)
	}

	// add or remove the selected cell to multi-selection
	func toggleIndexPathSelection(indexPath: IndexPath) {
		if selectedIndexPaths.contains(indexPath) {
			selectedIndexPaths.remove(indexPath)
		} else {
			selectedIndexPaths.insert(indexPath)
		}
		if selectedIndexPaths.isEmpty {
			toggleMultiSelect(toggle: false)
		}
		tableView.reloadData()
	}

	func toggleMultiSelect(toggle: Bool) {
		multiSelectMode = toggle
		footerView.isHidden = !multiSelectMode
		selectedIndexPaths.removeAll()
		tableView.reloadData()
	}

	func onServerDisconnect() {
		DispatchQueue.main.async {
			self.navigationController?.popViewController(animated: true)
		}
	}

	/// Enter multi-select mode
	@objc func onLongPress(longPressGestureRecognizer: UILongPressGestureRecognizer) {
		if longPressGestureRecognizer.state == UIGestureRecognizer.State.began {
			let touchPoint = longPressGestureRecognizer.location(in: tableView)
			if let indexPath = tableView.indexPathForRow(at: touchPoint) {
				if !multiSelectMode {
					toggleMultiSelect(toggle: true)
				}
				toggleIndexPathSelection(indexPath: indexPath)
			}
		}
	}
}
