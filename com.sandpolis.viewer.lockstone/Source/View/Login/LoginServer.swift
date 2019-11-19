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

class LoginServer: UIViewController {

	@IBOutlet weak var address: UITextField!
	@IBOutlet weak var username: UITextField!
	@IBOutlet weak var password: UITextField!

	var loginContainer: Login!
	
	override func viewDidLoad() {
		super.viewDidLoad()
		
		address.addTarget(self, action: #selector(refreshAddress), for: .editingChanged)
		username.addTarget(self, action: #selector(refreshUsername), for: .editingChanged)
		password.addTarget(self, action: #selector(refreshPassword), for: .editingChanged)

		address.setLeftIcon("field/server")
		username.setLeftIcon("field/username")
		password.setLeftIcon("field/password")
	}

	override func viewDidDisappear(_ animated: Bool) {
		// Clean up fields
		address.text = nil
		username.text = nil
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
		// TODO
	}

	@IBAction func openLoginAccount(_ sender: Any) {
		loginContainer.openLoginAccount()
	}
	
	@objc func refreshAddress() {
		// TODO
	}
	
	@objc func refreshUsername() {
		// TODO
	}
	
	@objc func refreshPassword() {
		// TODO
	}
}
