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
import FirebaseFirestore
import SwiftValidators

class AddServer: UIViewController, UITextFieldDelegate {

	@IBOutlet weak var titleLabel: UINavigationItem!
	@IBOutlet weak var name: UITextField!
	@IBOutlet weak var address: UITextField!
	@IBOutlet weak var username: UITextField!
	@IBOutlet weak var password: UITextField!
	@IBOutlet weak var status: UILabel!
	@IBOutlet weak var cloudButton: UIButton!

	var server: SandpolisServer!

	var serverReference: DocumentReference!

	override func viewDidLoad() {
		super.viewDidLoad()

		status.text = nil
		if server != nil {
			name.text = server.name
			address.text = server.address
			username.text = server.username
			password.text = server.password
			cloudButton.isHidden = true
			titleLabel.title = "Edit Server"
		}

		// Search for unused subscriptions
		cloudButton.setTitle(true ? "Don't have your own server yet?" : "Launch new cloud server", for: .normal)

		name.delegate = self
		name.tag = 0

		address.addTarget(self, action: #selector(refreshAddress), for: .editingChanged)
		address.delegate = self
		address.tag = 1

		username.addTarget(self, action: #selector(refreshUsername), for: .editingChanged)
		username.delegate = self
		username.tag = 2

		password.addTarget(self, action: #selector(refreshPassword), for: .editingChanged)
		password.delegate = self
		password.tag = 3

		refreshAddress()
		refreshUsername()
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

	@IBAction func save(_ sender: Any) {
		guard (Validator.isFQDN() || Validator.isIPv4()).apply(address.text) else {
			status.text = "Invalid address"
			return
		}
		guard Validator.minLength(4).apply(username.text) else {
			status.text = "Invalid username"
			return
		}
		guard Validator.minLength(8).apply(password.text) else {
			status.text = "Invalid password"
			return
		}
		guard Validator.minLength(1).apply(name.text) else {
			status.text = "Invalid name"
			return
		}

		serverReference.setData([
			"name": name.text!,
			"address": address.text!,
			"username": username.text!,
			"password": password.text!,
			"cloud": false
		])

		dismiss(animated: true, completion: nil)
	}

	@IBAction func cancelButtonPressed(_ sender: Any) {
		dismiss(animated: true, completion: nil)
	}

	@IBAction func cloudButtonPressed(_ sender: Any) {
		performSegue(withIdentifier: "CreateServerSegue", sender: self)
		//performSegue(withIdentifier: "PricingSegue", sender: self)
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
