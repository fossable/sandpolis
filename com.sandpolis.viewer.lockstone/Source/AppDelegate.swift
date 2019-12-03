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
		if let connection = SandpolisUtil.connection, connection.isConnected() {
			connection.disconnect()
		}
	}

	func applicationDidBecomeActive(_ application: UIApplication) {
	}

	func applicationWillTerminate(_ application: UIApplication) {
	}

	private static var configured = false

	/// Ensure that Firebase is configured
	static func requireFirebase() {
		if !configured {
			FirebaseApp.configure()
			configured = true
		}
	}
}
