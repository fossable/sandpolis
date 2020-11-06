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

class ControlPanel: UITabBarController {

	var profile: SandpolisProfile!

	override func viewDidLoad() {
		super.viewDidLoad()
		for controller in viewControllers! {
			if let overview = controller as? Overview {
				overview.profile = profile
			}
			if let fileManager = controller as? FileManager {
				fileManager.profile = profile
			}
			if let actions = controller as? Actions {
				actions.profile = profile
			}
			if let remoteDesktop = controller as? RemoteDesktop {
				remoteDesktop.profile = profile
			}
			if let shellSession = controller as? ShellSession {
				shellSession.profile = profile
			}
		}

		// Always select overview
		self.selectedIndex = 0
	}
}