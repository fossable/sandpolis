//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class GroupOverview: UIViewController, UITableViewDelegate, UITableViewDataSource {

	@IBOutlet weak var table: UITableView!
	var hostList: AgentList!
	var profiles = [SandpolisProfile]()

	override func viewDidLoad() {
		super.viewDidLoad()
		table.delegate = self
		table.dataSource = self
		navigationItem.title = "\(profiles.count) hosts selected"
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "StatsTable",
			let dest = segue.destination as? GroupPanelTable {
			dest.profiles = profiles
		} else if segue.identifier == "MacroSegue",
			let dest = segue.destination as? MacroSelect {
			dest.profiles = profiles
		} else if segue.identifier == "SingleControlPanelSegue",
			let dest = segue.destination as? ControlPanel {
			dest.profile = profiles[table.indexPathForSelectedRow!.row]
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
}
