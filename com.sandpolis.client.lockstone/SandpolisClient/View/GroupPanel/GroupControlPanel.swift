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
