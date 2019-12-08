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
import Firebase
import FirebaseAuth
import SwiftKeychainWrapper
import SwiftValidators

class LoginServer: UIViewController, UITextFieldDelegate {

	@IBOutlet weak var address: UITextField!
	@IBOutlet weak var username: UITextField!
	@IBOutlet weak var password: UITextField!

	var loginContainer: Login!

	override func viewDidLoad() {
		super.viewDidLoad()

		address.addTarget(self, action: #selector(refreshAddress), for: .editingChanged)
		address.delegate = self
		address.tag = 0

		username.addTarget(self, action: #selector(refreshUsername), for: .editingChanged)
		username.delegate = self
		username.tag = 1

		password.addTarget(self, action: #selector(refreshPassword), for: .editingChanged)
		password.delegate = self
		password.tag = 2

		refreshAddress()
		refreshUsername()
		refreshPassword()
	}

	override func viewDidDisappear(_ animated: Bool) {
		// Clean up fields
		address.text = nil
		username.text = nil
		password.text = nil
	}

	func textFieldShouldReturn(_ textField: UITextField) -> Bool {
		if let next = textField.superview?.viewWithTag(textField.tag + 1) as? UITextField {
			next.becomeFirstResponder()
		} else {
			textField.resignFirstResponder()
		}
		return false
	}

	override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
		view.endEditing(true)
	}

	@IBAction func login(_ sender: Any) {
		login(address: address.text!, username: username.text!, password: password.text!) { connection in
			if let connection = connection {
				SandpolisUtil.connection = connection
				connection.openProfileStream()

				UserDefaults.standard.set("direct", forKey: "login.type")
				UserDefaults.standard.set(true, forKey: "login.auto")

				DispatchQueue.main.async {
					KeychainWrapper.standard.set(self.address.text!, forKey: "login.direct.address")
					KeychainWrapper.standard.set(self.username.text!, forKey: "login.direct.username")
					KeychainWrapper.standard.set(self.password.text!, forKey: "login.direct.password")

					self.loginContainer.performSegue(withIdentifier: "DirectLoginCompleteSegue", sender: self.address.text!)
				}
			}
		}
	}

	@IBAction func openLoginAccount(_ sender: Any) {
		loginContainer.openLoginAccount()
	}

	@objc func refreshAddress() {
		if (Validator.isFQDN() || Validator.isIPv4()).apply(address.text) {
			address.setLeftIcon("field/server_selected")
		} else {
			address.setLeftIcon("field/server")
		}
	}

	@objc func refreshUsername() {
		if Validator.minLength(4).apply(username.text) {
			username.setLeftIcon("field/username_selected")
		} else {
			username.setLeftIcon("field/username")
		}
	}

	@objc func refreshPassword() {
		if Validator.minLength(8).apply(password.text) {
			password.setLeftIcon("field/password_selected")
		} else {
			password.setLeftIcon("field/password")
		}
	}
}
