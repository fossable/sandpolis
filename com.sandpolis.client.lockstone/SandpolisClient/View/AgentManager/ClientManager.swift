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
import SwiftEventBus
import os

class ClientManager: UITabBarController {

	var loginType: LoginType!

	enum LoginType {
		case direct(String)
		case cloud(SandpolisServer)
	}

	override func viewDidLoad() {
		super.viewDidLoad()

		switch loginType {
		case .direct(let name):
			navigationItem.title = name
			navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Logout", style: .plain, target: self, action: #selector(logoutDirect))
		case .cloud(let server):
			navigationItem.title = server.name
			navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .add, target: self, action: #selector(addClient))
		default:
			os_log("Unknown login type")
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

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "ClientScannerSegue",
			let dest = segue.destination as? ClientScanner {
			switch loginType {
			case .cloud(let server):
				dest.server = server
			default:
				break
			}
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
