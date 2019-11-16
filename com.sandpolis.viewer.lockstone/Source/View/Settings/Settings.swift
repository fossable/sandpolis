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
import Firebase
import FirebaseAuth
import Highlightr
import os

class Settings: UITableViewController {

	@IBOutlet weak var hostView: UISegmentedControl!
	@IBOutlet weak var mapLocation: UISegmentedControl!

	override func viewDidLoad() {
		if let defaultView = UserDefaults.standard.string(forKey: "default_view") {
			switch defaultView {
			case "list":
				hostView.selectedSegmentIndex = 0
			case "map":
				hostView.selectedSegmentIndex = 1
			default:
				break
			}
		}

		if UserDefaults.standard.bool(forKey: "map.location") {
			mapLocation.selectedSegmentIndex = 0
		} else {
			mapLocation.selectedSegmentIndex = 1
		}
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		tableView.deselectRow(at: indexPath, animated: true)

		if indexPath.row == 2 {
			// Logout
			do {
				try Auth.auth().signOut()
			} catch {
				os_log("Failed to logout: %s", error.localizedDescription)
			}
			performSegue(withIdentifier: "UnwindLoginSegue", sender: self)
		}
	}

	@IBAction func hostViewChanged(_ sender: Any) {
		switch hostView.selectedSegmentIndex {
		case 0:
			UserDefaults.standard.set("list", forKey: "default_view")
		case 1:
			UserDefaults.standard.set("map", forKey: "default_view")
		default:
			break
		}
	}

	@IBAction func mapLocationChanged(_ sender: Any) {
		switch mapLocation.selectedSegmentIndex {
		case 0:
			UserDefaults.standard.set(true, forKey: "map.location")
		case 1:
			UserDefaults.standard.set(false, forKey: "map.location")
		default:
			break
		}
	}
}
