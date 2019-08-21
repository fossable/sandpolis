/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
import UIKit
import Firebase

class ChangePassword: UIViewController {

	@IBOutlet weak var CurrentPassword: UITextField!
	@IBOutlet weak var NewPassword: UITextField!
	@IBOutlet weak var ConfirmNewPassword: UITextField!

	@IBAction func SavePassword(_ sender: Any) {
		if NewPassword.text != ConfirmNewPassword.text {
			let controller = UIAlertController(
				title: "Inconsistent Password",
				message: "Please try again",
				preferredStyle: .alert)
			controller.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
			present(controller, animated: true, completion: nil)
			return
		}
		if let password = NewPassword.text {
			Auth.auth().currentUser?.updatePassword(to: password)
		}
		self.navigationController?.popViewController(animated: true)
		self.dismiss(animated: true, completion: nil)
	}
}
