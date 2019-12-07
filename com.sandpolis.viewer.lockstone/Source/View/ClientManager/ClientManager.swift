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
import SwiftEventBus

class ClientManager: UITabBarController {

	/// The server title
	var serverName: String?

	override func viewDidLoad() {
		super.viewDidLoad()
		
		// Set navigation bar
		navigationItem.title = serverName
		if UserDefaults.standard.string(forKey: "login.type") == "direct" {
			navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Logout", style: .plain, target: self, action: #selector(logoutDirect))
		}

		// Set default view
		if let defaultView = UserDefaults.standard.string(forKey: "default_view") {
			switch defaultView {
			case "list":
				self.selectedIndex = 0
			case "map":
				self.selectedIndex = 1
			default:
				break
			}
		}

		SwiftEventBus.unregister(self)
		SwiftEventBus.onMainThread(self, name: "connectionLostEvent") { _ in
			SwiftEventBus.unregister(self)
			if UserDefaults.standard.string(forKey: "login.type") == "direct" {
				let alert = UIAlertController(title: "Connection lost", message: "Your connection to the server has been lost.", preferredStyle: .alert)
				//alert.addAction(UIAlertAction(title: "Reconnect", style: .default) { _ in
				//})
				alert.addAction(UIAlertAction(title: "Exit", style: .cancel) { _ in
					self.performSegue(withIdentifier: "UnwindLoginSegue", sender: self)
				})
				self.present(alert, animated: true)
			} else {
				self.performSegue(withIdentifier: "UnwindServerSegue", sender: self)
			}
		}
	}

	@objc func logoutDirect() {
		SwiftEventBus.unregister(self)
		SandpolisUtil.connection.disconnect()
		performSegue(withIdentifier: "UnwindLoginSegue", sender: self)
	}
}
