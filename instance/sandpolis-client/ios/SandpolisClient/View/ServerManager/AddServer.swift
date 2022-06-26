//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit
import SwiftValidators
import SwiftKeychainWrapper

class AddServer: UIViewController, UITextFieldDelegate {

	@IBOutlet weak var titleLabel: UINavigationItem!
	@IBOutlet weak var name: UITextField!
	@IBOutlet weak var address: UITextField!
	@IBOutlet weak var username: UITextField!
	@IBOutlet weak var password: UITextField!
	@IBOutlet weak var status: UILabel!

	var server: SandpolisServer!

	override func viewDidLoad() {
		super.viewDidLoad()

		status.text = nil
		if server != nil {
			name.text = server.name
			address.text = server.address
			username.text = server.username
			titleLabel.title = "Edit Server"
		}

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

		// Save credentials to keychain
		KeychainWrapper.standard.set(self.password.text!, forKey: "server.\(self.address.text!).password")

		// Save server metadata to user defaults
		var servers = UserDefaults.standard.stringArray(forKey: "servers") ?? []
		servers.append("""
			{
				"address": "\(self.address.text!)",
				"username": "\(self.username.text!)",
				"name": "\(self.name.text!)"
			}
		""")
		UserDefaults.standard.set(servers, forKey: "servers")

		dismiss(animated: true, completion: nil)
	}

	@IBAction func cancelButtonPressed(_ sender: Any) {
		dismiss(animated: true, completion: nil)
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
