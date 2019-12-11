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
