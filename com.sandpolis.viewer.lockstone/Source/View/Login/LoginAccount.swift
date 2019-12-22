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
import FirebaseAuth
import SwiftValidators

class LoginAccount: UIViewController, UITextFieldDelegate {

	@IBOutlet weak var email: UITextField!
	@IBOutlet weak var password: UITextField!
	@IBOutlet weak var loginButton: UIButton!
	@IBOutlet weak var forgotPasswordButton: UIButton!
	@IBOutlet weak var createAccountButton: UIButton!

	var loginContainer: Login!

	override func viewDidLoad() {
		super.viewDidLoad()

		email.addTarget(self, action: #selector(refreshEmail), for: .editingChanged)
		email.delegate = self
		email.tag = 0

		password.addTarget(self, action: #selector(refreshPassword), for: .editingChanged)
		password.delegate = self
		password.tag = 1

		refreshEmail()
		refreshPassword()
	}

	override func viewDidDisappear(_ animated: Bool) {
		// Clean up fields
		email.text = nil
		password.text = nil

		refreshEmail()
		refreshPassword()
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
		AppDelegate.requireFirebase()
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
				AppDelegate.requireFirebase()
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
		if Validator.isEmail().apply(email.text) {
			email.setLeftIcon("field/email_selected")
		} else {
			email.setLeftIcon("field/email")
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
