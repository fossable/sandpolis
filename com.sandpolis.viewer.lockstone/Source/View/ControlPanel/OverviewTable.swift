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

class OverviewTable: UITableViewController {

	var profile: SandpolisProfile!

	/*var info: [(String, [Attribute])] = [
		("System", [Attribute("/system/uptime", "Uptime")]),
		("CPU", [Attribute("/cpu/_/model", "Model"), Attribute("/cpu/_/max_frequency", "Max Frequency")]),
		("Memory", [Attribute("/memory/usage", "Usage"), Attribute("/memory/swap/usage", "Swap Usage")]),
		("Processes", [Attribute("/process/count", "Number of Processes")])
	]*/
	
	var info: [(String, [Attribute])] = [
		("System", [Attribute("", "Operating System"), Attribute("", "Hostname")]),
		("Location", [Attribute("", "Latitude"), Attribute("", "Longitude"), Attribute("", "City"), Attribute("", "Country"), Attribute("", "ISP")])
	]

	/// The table update timer
	private var updater: Timer!

	override func viewDidLoad() {
		super.viewDidLoad()
		tableView.allowsSelection = false
		
		// Temporarily set values
		switch profile.platform {
		case .linux:
			info[0].1[0].value = "Linux"
		case .macos:
			info[0].1[0].value = "macOS"
		case .windows:
			info[0].1[0].value = "Windows"
		case .freebsd:
			info[0].1[0].value = "FreeBSD"
		default:
			info[0].1[0].value = "Unknown OS"
		}
		info[0].1[1].value = profile.hostname
		if let location = profile.location {
			info[1].1[0].value = String(location.latitude)
			info[1].1[1].value = String(location.longitude)
			info[1].1[2].value = location.city
			info[1].1[3].value = location.country
			info[1].1[4].value = location.isp
		}
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
		cell.setAttribute(info[indexPath.section].1[indexPath.row])
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
