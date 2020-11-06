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
import SwiftValidators
import FirebaseAuth
import FirebaseFirestore

class CreateAccount: UIViewController, UITextFieldDelegate {

	@IBOutlet weak var email: UITextField!
	@IBOutlet weak var password: UITextField!

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

	@IBAction func create(_ sender: Any) {
		AppDelegate.requireFirebase()
		Auth.auth().createUser(withEmail: email.text!, password: password.text!) { authResult, error in
			if error == nil {
				Auth.auth().signIn(withEmail: self.email.text!, password: self.password.text!) { result, error in
					if error != nil {
						let alert = UIAlertController(title: "Error", message: error?.localizedDescription, preferredStyle: .alert)
						alert.addAction(UIAlertAction(title: "OK", style: .default))

						self.present(alert, animated: true, completion: nil)
					} else {
						self.copyDefaults()
						UserDefaults.standard.set("cloud", forKey: "login.type")
						UserDefaults.standard.set(true, forKey: "login.auto")
						self.loginContainer.performSegue(withIdentifier: "LoginCompleteSegue", sender: nil)
					}
				}
			} else {
				let alert = UIAlertController(title: "Error", message: error?.localizedDescription, preferredStyle: .alert)
				alert.addAction(UIAlertAction(title: "OK", style: .default))

				self.present(alert, animated: true, completion: nil)
			}
		}
	}

	@IBAction func openLoginAccount(_ sender: Any) {
		loginContainer.openLoginAccount()
	}

	@IBAction func openLoginServer(_ sender: Any) {
		loginContainer.openLoginServer()
	}

	@IBAction func openPrivacyPolicy(_ sender: Any) {
		if let url = URL(string: "https://sandpolis.com/privacy") {
			UIApplication.shared.open(url)
		}
	}

	/// Copy default user data on account creation
	private func copyDefaults() {
		let userServers = Firestore.firestore().collection("/user/\(Auth.auth().currentUser!.uid)/server")
		let defaultServers = Firestore.firestore().collection("/default/server/list")
		let userMacros = Firestore.firestore().collection("/user/\(Auth.auth().currentUser!.uid)/macro")
		let defaultMacros = Firestore.firestore().collection("/default/macro/list")

		defaultServers.getDocuments { querySnapshot, error in
			guard let servers = querySnapshot?.documents else {
				return
			}

			for server in servers {
				userServers.addDocument(data: server.data())
			}
		}

		defaultMacros.getDocuments { querySnapshot, error in
			guard let macros = querySnapshot?.documents else {
				return
			}

			for macro in macros {
				userMacros.addDocument(data: macro.data())
			}
		}
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
