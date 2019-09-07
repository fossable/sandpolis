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
import FirebaseAuth

class SignInAccount: UIViewController {

	@IBOutlet weak var textFieldLoginEmail: UITextField!
	@IBOutlet weak var textFieldLoginPassword: UITextField!
	@IBOutlet weak var loginButton: UIButton!
	@IBOutlet weak var forgotPasswordButton: UIButton!
	@IBOutlet weak var createAccountButton: UIButton!

	var loginContainer: Login!

	override func viewDidDisappear(_ animated: Bool) {
		// Clean up fields
		textFieldLoginEmail.text = nil
		textFieldLoginPassword.text = nil
	}

	func textFieldShouldReturn(textField: UITextField) -> Bool {
		textField.resignFirstResponder()
		return true
	}

	override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
		self.view.endEditing(true)
	}

	@IBAction func loginDidTouch(_ sender: UIButton) {
		guard
			let email = textFieldLoginEmail.text,
			let password = textFieldLoginPassword.text,
			email.count > 0,
			password.count > 0
			else {
				return
		}

		Auth.auth().signIn(withEmail: self.textFieldLoginEmail.text!, password: self.textFieldLoginPassword.text!) { (user, error) in
			if error != nil {
				let alert = UIAlertController(title: "Sign In Failed", message: error?.localizedDescription, preferredStyle: .alert)
				alert.addAction(UIAlertAction(title: "OK", style: .default))

				self.present(alert, animated: true, completion: nil)
			}
		}
	}

	@IBAction func createAccount(_ sender: Any) {
		loginContainer.openCreateAccount()
	}

	@IBAction func forgotPassword(_ sender: Any) {
		loginContainer.openForgotPassword()
	}

}
