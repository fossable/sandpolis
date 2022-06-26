//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class OverviewTable: UITableViewController {

	var profile: SandpolisProfile!

	/*var info: [(String, [Attribute])] = [
		("System", [Attribute("/system/uptime", "Uptime")]),
		("CPU", [Attribute("/cpu/_/model", "Model"), Attribute("/cpu/_/max_frequency", "Max Frequency")]),
		("Memory", [Attribute("/memory/usage", "Usage"), Attribute("/memory/swap/usage", "Swap Usage")]),
		("Processes", [Attribute("/process/count", "Number of Processes")])
	]*/

	var info: [(String, [Oid])] = [
	//    ("System", [InstanceOid.osType, InstanceOid.hostname]),
//        ("Location", [InstanceOid.ipLocationCountry])
	]

	/// The table update timer
	private var updater: Timer!

	override func viewDidLoad() {
		super.viewDidLoad()
		tableView.allowsSelection = false

		// Temporarily set values
		/*switch profile.osFamily.value {
		case .linux:
			info[0].1[0].value = "Linux"
		case .darwin:
			info[0].1[0].value = "macOS"
		case .windows:
			info[0].1[0].value = "Windows"
		case .bsd:
			info[0].1[0].value = "FreeBSD"
		default:
			info[0].1[0].value = "Unknown OS"
		}
		info[0].1[1].value = profile.hostname.value
		info[1].1[0].value = "\(profile.ipLocationLatitude.value!)"
		info[1].1[1].value = "\(profile.ipLocationLongitude.value!)"
		info[1].1[2].value = profile.ipLocationCity.value
		info[1].1[3].value = profile.ipLocationCountry.value
		info[1].1[4].value = profile.ipLocationIsp.value*/
	}

	override func viewWillAppear(_ animated: Bool) {
		// Start updating
		update()
		updater = Timer.scheduledTimer(timeInterval: 1, target: self, selector: #selector(update), userInfo: nil, repeats: true)
	}

	override func viewWillDisappear(_ animated: Bool) {
		// Stop updating
		updater.invalidate()
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let cell = tableView.dequeueReusableCell(withIdentifier: "InfoCell") as! InfoCell
		//cell.setAttribute(info[indexPath.section].1[indexPath.row])
		return cell
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return info[section].1.count
	}

	override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
		return info[section].0
	}

	override func numberOfSections(in tableView: UITableView) -> Int {
		return info.count
	}

	@objc private func update() {
		/*if let start = profile.startTime {
			uptime.text = FormatUtil.timeSince(start)
		}
		if let timezone = profile.timezone {
			time.text = FormatUtil.formatDateInTimezone(Date(), timezone)
		} else {
			time.text = "Unknown timezone"
		}*/
	}
}
