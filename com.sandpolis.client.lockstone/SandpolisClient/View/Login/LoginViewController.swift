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

class SignInAccount: UIViewController, GIDSignInUIDelegate {

	@IBOutlet weak var textFieldLoginEmail: UITextField!
	@IBOutlet weak var textFieldLoginPassword: UITextField!
	@IBOutlet weak var googleSignInButton: GIDSignInButton!
	@IBOutlet weak var loginButton: UIButton!
	@IBOutlet weak var forgotPasswordButton: UIButton!
	@IBOutlet weak var createAccountButton: UIButton!

	var loginContainer: Login!

	override func viewDidLoad() {
		super.viewDidLoad()

		GIDSignIn.sharedInstance().uiDelegate = self
	}

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
			if error == nil {
				self.loginContainer.performSegue(withIdentifier: "LoginCompleteSegue", sender: self)
			}
			else {
				//Tells the user that there is an error and then gets firebase to tell them the error
				let alert = UIAlertController(title: "Sign In Failed",
					message: error?.localizedDescription,
					preferredStyle: .alert)

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
