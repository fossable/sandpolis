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

class Overview: UIViewController {

	@IBOutlet weak var platform: UIImageView!
	@IBOutlet weak var hostname: UILabel!
	@IBOutlet weak var location: UILabel!
	@IBOutlet weak var screenshot: UIImageView!
	@IBOutlet weak var screenshotMessage: UILabel!
	@IBOutlet weak var tableContainer: UIView!
	@IBOutlet weak var screenshotProgress: UIActivityIndicatorView!

	var profile: SandpolisProfile!

	override func viewDidLoad() {
		super.viewDidLoad()

		// Set screenshot if it already exists
		if let img = self.profile.screenshot {
			self.screenshot.image = UIImage(data: img)
		} else {
			// Trigger refresh
			refreshScreenshot()
		}

		// Set profile information
		hostname.text = profile.hostname
		location.text = FormatUtil.formatProfileLocation(profile)
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "EmbeddedTable",
			let table = segue.destination as? OverviewTable {
			table.profile = profile
		} else if segue.identifier == "FileManagerSegue",
			let fileManager = segue.destination as? FileManager {
			fileManager.profile = profile
		} else if segue.identifier == "MacroSegue",
			let macroSelect = segue.destination as? MacroSelect {
			macroSelect.profiles.append(profile)
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	@IBAction func screenshotTapped(_ sender: Any) {
		refreshScreenshot()
	}

	/// Refresh the screenshot view
	private func refreshScreenshot() {
		screenshotProgress.startAnimating()
		SandpolisUtil.connection.screenshot(profile.cvid).whenSuccess { rs in
			if rs.rsScreenshot.data.count == 0 {
				DispatchQueue.main.async {
					self.screenshotProgress.stopAnimating()
					self.screenshotMessage.isHidden = false
				}
			} else {
				self.profile.screenshot = rs.rsScreenshot.data
				DispatchQueue.main.async {
					self.screenshotProgress.stopAnimating()
					self.screenshot.image = UIImage(data: self.profile.screenshot!)
					self.screenshotMessage.isHidden = true
				}
			}
		}
	}
}
