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

class MainTabController: UITabBarController {

	var server: SandpolisServer!

	override func viewDidLoad() {
		super.viewDidLoad()
		navigationItem.title = server.name
		let hostListVC = viewControllers![0] as! ClientList
		hostListVC.server = server
		let listFirst = UserDefaults.standard.integer(forKey: "defaultView")
		if listFirst == 0 {
			self.selectedIndex = 0
		} else {
			self.selectedIndex = 1
		}
	}
}
