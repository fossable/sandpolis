/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
import UIKit

class AddServer: UIViewController {

    @IBOutlet weak var titleLabel: UINavigationItem!
    @IBOutlet weak var nameTextField: UITextField!
    @IBOutlet weak var addressTextField: UITextField!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var errorLabel: UILabel!

    var serverListViewController: ServerManager!
    var editMode = false
    var originalName = ""
    var originalServer: SandpolisServer!

    func setEditMode(server: SandpolisServer) {
        editMode = true
        originalName = server.name
        originalServer = server
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        errorLabel.text = ""
        if editMode {
            nameTextField.text = originalServer.name
            addressTextField.text = originalServer.address
            usernameTextField.text = originalServer.username
            passwordTextField.text = originalServer.password
            titleLabel.title = "Edit Server"
        }
    }

    @IBAction func saveButtonPressed(_ sender: Any) {
        if nameTextField.text!.isEmpty || addressTextField.text!.isEmpty || usernameTextField.text!.isEmpty || passwordTextField.text!.isEmpty {
            errorLabel.text = "Please fill out all fields."
            return
        }
        let error = serverListViewController.addServer(edit: editMode, originalName: originalName, name: nameTextField.text!, address: addressTextField.text!, username: usernameTextField.text!, password: passwordTextField.text!)
        if error == "" {
            dismiss(animated: true, completion: nil)
        } else {
            errorLabel.text = error
        }
    }

    @IBAction func cancelButtonPressed(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }

    func textFieldShouldReturn(textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.view.endEditing(true)
    }
}
