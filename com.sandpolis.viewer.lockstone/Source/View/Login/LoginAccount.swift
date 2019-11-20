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

class LoginAccount: UIViewController {

	@IBOutlet weak var email: UITextField!
	@IBOutlet weak var password: UITextField!
	@IBOutlet weak var loginButton: UIButton!
	@IBOutlet weak var forgotPasswordButton: UIButton!
	@IBOutlet weak var createAccountButton: UIButton!

	var loginContainer: Login!

	override func viewDidLoad() {
		super.viewDidLoad()

		email.addTarget(self, action: #selector(refreshEmail), for: .editingChanged)
		password.addTarget(self, action: #selector(refreshPassword), for: .editingChanged)

		email.setLeftIcon("field/email")
		password.setLeftIcon("field/password")
	}

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

	@IBAction func login(_ sender: Any) {
		AppDelegate.ensureFirebase()
		Auth.auth().signIn(withEmail: self.email.text!, password: self.password.text!) { (user, error) in
			if error != nil {
				let alert = UIAlertController(title: "Sign In Failed", message: error?.localizedDescription, preferredStyle: .alert)
				alert.addAction(UIAlertAction(title: "OK", style: .default))

				self.present(alert, animated: true, completion: nil)
			} else if user != nil {
				UserDefaults.standard.set("cloud", forKey: "login.type")
				UserDefaults.standard.set(true, forKey: "login.auto")
				self.loginContainer.performSegue(withIdentifier: "LoginCompleteSegue", sender: nil)
			}
		}
	}

	@IBAction func openCreateAccount(_ sender: Any) {
		loginContainer.openCreateAccount()
	}

	@IBAction func openResetPassword(_ sender: Any) {
		let alert = UIAlertController(title: "Reset account password", message: "Enter your email address", preferredStyle: .alert)
		alert.addTextField { field in
			field.placeholder = "Email"
		}
		alert.addAction(UIAlertAction(title: "OK", style: .default) { [weak alert] _ in
			if let email = alert?.textFields?[0].text {
				AppDelegate.ensureFirebase()
				Auth.auth().sendPasswordReset(withEmail: email) { _ in
					// TODO
				}
			}
		})
		alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
		present(alert, animated: true)
	}

	@IBAction func openLoginServer(_ sender: Any) {
		loginContainer.openLoginServer()
	}
	
	@objc func refreshEmail() {
		// TODO
	}
	
	@objc func refreshPassword() {
		// TODO
	}
}
