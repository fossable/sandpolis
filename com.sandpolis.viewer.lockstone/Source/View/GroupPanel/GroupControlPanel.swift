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

class GroupControlPanel: UITabBarController {

	var profiles: [SandpolisProfile]!

	override func viewDidLoad() {
		super.viewDidLoad()
		for controller in viewControllers! {
			if let overview = controller as? GroupOverview {
				overview.profiles = profiles
			}
			if let actions = controller as? GroupActions {
				actions.profiles = profiles
			}
		}

		// Always select overview
		self.selectedIndex = 0
		
		// Set title
		self.navigationItem.title = "\(profiles.count) hosts selected"
	}
}
