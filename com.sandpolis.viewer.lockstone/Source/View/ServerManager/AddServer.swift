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

class AddServer: UIViewController {

    @IBOutlet weak var titleLabel: UINavigationItem!
    @IBOutlet weak var nameTextField: UITextField!
    @IBOutlet weak var addressTextField: UITextField!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var errorLabel: UILabel!
    @IBOutlet weak var cloudButton: UIButton!

    var server: SandpolisServer!

	var serverReference: DocumentReference!

    override func viewDidLoad() {
        super.viewDidLoad()
        errorLabel.text = nil
        if server != nil {
            nameTextField.text = server.name
            addressTextField.text = server.address
            usernameTextField.text = server.username
            passwordTextField.text = server.password
            cloudButton.isHidden = true
            titleLabel.title = "Edit Server"
        }

		// Search for unused subscriptions
        cloudButton.setTitle(true ? "Don't have your own server yet?" : "Launch new cloud server", for: .normal)

    }

	override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
		self.view.endEditing(true)
	}

    @IBAction func saveButtonPressed(_ sender: Any) {
        if nameTextField.text!.isEmpty || addressTextField.text!.isEmpty || usernameTextField.text!.isEmpty || passwordTextField.text!.isEmpty {
            errorLabel.text = "Please fill out all fields."
            return
        }

		serverReference.setData([
			"name": nameTextField.text!,
			"address": addressTextField.text!,
			"username": usernameTextField.text!,
			"password": passwordTextField.text!,
			"cloud": false
		])

        dismiss(animated: true, completion: nil)
    }

    @IBAction func cancelButtonPressed(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }

    @IBAction func cloudButtonPressed(_ sender: Any) {
        performSegue(withIdentifier: "PricingSegue", sender: self)
    }

    func textFieldShouldReturn(textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
}
