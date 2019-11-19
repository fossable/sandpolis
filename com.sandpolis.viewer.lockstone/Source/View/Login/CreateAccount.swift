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
import FirebaseFirestore

class CreateAccount: UIViewController {

	@IBOutlet weak var email: UITextField!
	@IBOutlet weak var password: UITextField!

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

	@IBAction func create(_ sender: Any) {
		Auth.auth().createUser(withEmail: email.text!, password: password.text!) { authResult, error in
			if error == nil {
				Auth.auth().signIn(withEmail: self.email.text!, password: self.password.text!) { result, error in
					if error != nil {
						let alert = UIAlertController(title: "Error", message: error?.localizedDescription, preferredStyle: .alert)
						alert.addAction(UIAlertAction(title: "OK", style: .default))

						self.present(alert, animated: true, completion: nil)
					} else {
						self.copyDefaults()
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
		// TODO
	}
	
	@objc func refreshPassword() {
		// TODO
	}
}
