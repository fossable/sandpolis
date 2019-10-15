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

	@IBOutlet weak var platform: UILabel!
	@IBOutlet weak var uptime: UILabel!
	@IBOutlet weak var username: UILabel!
	@IBOutlet weak var time: UILabel!
	@IBOutlet weak var ip: UILabel!
	@IBOutlet weak var upload: UILabel!
	@IBOutlet weak var download: UILabel!

	var profile: SandpolisProfile!

	/// The table update timer
	private var updater: Timer!

	override func viewDidLoad() {
		super.viewDidLoad()

		tableView.allowsSelection = false

		platform.text = profile.osVersion
		username.text = profile.username

		ip.text = profile.ipAddress
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

	@objc private func update() {
		if let start = profile.startTime {
			uptime.text = FormatUtil.timeSince(start)
		}
		if let timezone = profile.timezone {
			time.text = FormatUtil.formatDateInTimezone(Date(), timezone)
		} else {
			time.text = "Unknown timezone"
		}
	}
}
