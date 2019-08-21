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
import FirebaseAuth

class CreateAccount: UIViewController {

	@IBOutlet weak var email: UITextField!
	@IBOutlet weak var password: UITextField!

	var loginContainer: Login!

	override func viewDidDisappear(_ animated: Bool) {
		// Clean up fields
		email.text = nil
		password.text = nil
	}

	func textFieldShouldReturn(textField: UITextField) -> Bool {
		textField.resignFirstResponder()
		return true
	}

	override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
		self.view.endEditing(true)
	}

	@IBAction func didTouchUpButton(_ sender: UIButton) {
		Auth.auth().createUser(withEmail: email.text!, password: password.text!) { authResult, error in
			if error == nil {
				Auth.auth().signIn(withEmail: self.email.text!, password: self.password.text!) { result, error in
					self.copyDefaults()
					self.loginContainer.performSegue(withIdentifier: "LoginCompleteSegue", sender: self)
				}
			} else {
				//Tells the user that there is an error and then gets firebase to tell them the error
				let alert = UIAlertController(title: "Error",
					message: error?.localizedDescription,
					preferredStyle: .alert)

				alert.addAction(UIAlertAction(title: "OK", style: .default))

				self.present(alert, animated: true, completion: nil)
			}
		}
	}

	@IBAction func login(_ sender: Any) {
		loginContainer.openLogin()
	}

	/// Copy default user data on account creation
	private func copyDefaults() {
		let userServers = Database.database().reference(withPath: "\(Auth.auth().currentUser!.uid)/servers")
		let defaultServers = Database.database().reference(withPath: "default/servers")
		let userMacros = Database.database().reference(withPath: "\(Auth.auth().currentUser!.uid)/macros")
		let defaultMacros = Database.database().reference(withPath: "default/macros")

		defaultServers.observeSingleEvent(of: .value) { snapshot in
			userServers.setValue(snapshot.value)
		}
		defaultMacros.observeSingleEvent(of: .value) { snapshot in
			userMacros.setValue(snapshot.value)
		}
	}
}
