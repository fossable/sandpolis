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

	@IBAction func saveButtonPressed(_ sender: Any) {
		if name.text!.isEmpty || address.text!.isEmpty || username.text!.isEmpty || password.text!.isEmpty {
			status.text = "Please fill out all fields."
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
