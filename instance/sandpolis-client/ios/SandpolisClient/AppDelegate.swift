//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit
import os

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

	var window: UIWindow?

	func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

		// Set UUID if necessary
		if UserDefaults.standard.string(forKey: "uuid") == nil {
			UserDefaults.standard.set(UUID().uuidString.lowercased(), forKey: "uuid")
		}

		return true
	}

	func applicationWillResignActive(_ application: UIApplication) {
	}

	func applicationDidBecomeActive(_ application: UIApplication) {
		if let connection = SandpolisUtil.connection, !connection.isConnected() {
			connection.disconnect()
			SandpolisUtil.connection = nil
		}
	}

	func applicationWillTerminate(_ application: UIApplication) {
		if let connection = SandpolisUtil.connection, connection.isConnected() {
			connection.disconnect()
		}
	}
}
