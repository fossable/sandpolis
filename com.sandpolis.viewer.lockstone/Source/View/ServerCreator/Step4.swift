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
