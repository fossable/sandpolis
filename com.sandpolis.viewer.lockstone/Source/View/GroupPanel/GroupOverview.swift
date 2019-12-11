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

class GroupOverview: UIViewController, UITableViewDelegate, UITableViewDataSource {

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
}
