//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit
import SwiftKeychainWrapper

class Login: UIViewController {

	override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
		get { return UIInterfaceOrientationMask.portrait }
	}

	override var shouldAutorotate: Bool {
		get { return false }
	}

	override var preferredInterfaceOrientationForPresentation: UIInterfaceOrientation {
		get { return UIInterfaceOrientation.portrait }
	}

	override var preferredStatusBarStyle: UIStatusBarStyle {
		return .lightContent
	}

	override func viewDidLoad() {
		super.viewDidLoad()

		if UserDefaults.standard.bool(forKey: "skip_welcome") == true {
			UserDefaults.standard.set(true, forKey: "skip_welcome")
			self.performSegue(withIdentifier: "LoginCompleteSegue", sender: nil)
		}
	}

	@IBAction func btnContinue(_ sender: Any) {
		UserDefaults.standard.set(true, forKey: "skip_welcome")
		self.performSegue(withIdentifier: "LoginCompleteSegue", sender: nil)
	}
}
