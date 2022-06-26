//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class Step4: UIViewController {

	@IBOutlet weak var launchButton: UIButton!
	@IBOutlet weak var status: UIActivityIndicatorView!

	var parentController: ServerCreator!


	@IBAction func back(_ sender: Any) {
		parentController.showStep3()
	}

	@IBAction func launch(_ sender: Any) {
		launchButton.isEnabled = false
		status.startAnimating()

		CloudUtil.createCloudInstance(hostname: parentController.step1.name.text!, username: parentController.step2.username.text!, password: parentController.step2.password.text!, location: parentController.step3.locationButton.titleLabel!.text!) { json, error in
			DispatchQueue.main.async {
				self.status.stopAnimating()
			}

			if let error = error {
				let alert = UIAlertController(title: "Launch failed", message: error.localizedDescription, preferredStyle: .alert)
				self.present(alert, animated: true) {

				}
			}
		}
	}
}
