//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit
import SwiftEventBus
import os

class AgentManager: UITabBarController {

	var server: SandpolisServer!

	override func viewDidLoad() {
		super.viewDidLoad()

		navigationItem.title = server.name
		navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .add, target: self, action: #selector(addClient))

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

			self.performSegue(withIdentifier: "UnwindServerSegue", sender: self)

			let alert = UIAlertController(title: "Connection lost", message: "Your connection to the server has been lost.", preferredStyle: .alert)
			self.present(alert, animated: true)
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "ClientScannerSegue",
			let dest = segue.destination as? AgentScanner {
			dest.server = server
		}
	}

	@objc func logoutDirect() {
		SwiftEventBus.unregister(self)
		SandpolisUtil.connection.disconnect()
		performSegue(withIdentifier: "UnwindLoginSegue", sender: self)
	}

	@objc func addClient() {
		performSegue(withIdentifier: "ClientScannerSegue", sender: self)
	}
}
