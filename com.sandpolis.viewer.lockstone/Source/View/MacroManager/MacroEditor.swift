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
import Highlightr
import FirebaseFirestore

class MacroEditor: UIViewController {

	@IBOutlet weak var platformSelector: UISegmentedControl!
	@IBOutlet weak var genericView: UIView!

	private let nameField: UITextField = UITextField(frame: CGRect(x: 0, y: 0, width: 210, height: 21))

	private let textStorage = CodeAttributedString()
	private var highlightr: Highlightr!
	private var textView: UITextView!
	private var saveButton: UIBarButtonItem!

	/// The macro being edited or nil for a new macro
	var macro: DocumentSnapshot!

	/// The macro reference
	var macroReference: DocumentReference!

	override func viewDidLoad() {
		super.viewDidLoad()

		nameField.placeholder = "Enter a macro name"
		nameField.textAlignment = .center
		nameField.borderStyle = .roundedRect
		nameField.addTarget(self, action: #selector(refreshSaveButton), for: .editingChanged)
		navigationItem.titleView = nameField

		saveButton = UIBarButtonItem(title: macro != nil ? "Update" : "Save", style: .done, target: self, action: #selector(saveMacro))
		navigationItem.rightBarButtonItem = saveButton

		let layoutManager = NSLayoutManager()
		textStorage.addLayoutManager(layoutManager)

		let textContainer = NSTextContainer(size: view.bounds.size)
		layoutManager.addTextContainer(textContainer)

		textView = UITextView(frame: genericView.bounds, textContainer: textContainer)
		textView.autoresizingMask = [.flexibleHeight, .flexibleWidth]
		textView.autocorrectionType = .no
		textView.autocapitalizationType = .none
		genericView.addSubview(textView)

		highlightr = textStorage.highlightr
		if let theme = UserDefaults.standard.string(forKey: "terminalTheme") {
			changeTheme(theme)
		} else {
			changeTheme("zenburn")
		}

		if macro != nil {
			nameField.text = macro["name"] as? String
			textView.text = macro["script"] as? String

			switch macro["type"] as! String {
			case "powershell":
				platformSelector.selectedSegmentIndex = 0
			case "cmd":
				platformSelector.selectedSegmentIndex = 1
			case "bash":
				platformSelector.selectedSegmentIndex = 2
			case "tcsh":
				platformSelector.selectedSegmentIndex = 3
			default:
				break
			}
			platformChanged(self)
		} else {
			// Default to bash
			platformSelector.selectedSegmentIndex = 2
			platformChanged(self)
		}
	}

	func textFieldShouldReturn(textField: UITextField) -> Bool {
		textField.resignFirstResponder()
		return true
	}

	override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
		self.view.endEditing(true)
	}

	@objc private func saveMacro(_ sender: Any) {
		var type: String = ""
		switch platformSelector.selectedSegmentIndex {
		case 0:
			type = "powershell"
		case 1:
			type = "cmd"
		case 2:
			type = "bash"
		case 3:
			type = "tcsh"
		default:
			break
		}

		macroReference.setData([
			"name": nameField.text!,
			"script": textView.text!,
			"type": type
		])

		navigationController?.popViewController(animated: true)
	}

	@IBAction func platformChanged(_ sender: Any) {
		switch platformSelector.selectedSegmentIndex {
		case 0:
			textStorage.language = "powershell"
		case 1:
			textStorage.language = "dos"
		case 2:
			textStorage.language = "bash"
		case 3:
			textStorage.language = "plaintext"
		default:
			break
		}

		refreshSaveButton()
	}

	private func changeTheme(_ theme: String) {
		highlightr.setTheme(to: theme)

		textView.backgroundColor = highlightr.theme.themeBackgroundColor
	}

	@objc private func refreshSaveButton() {
		if nameField.text!.count > 0 {
			saveButton.isEnabled = true
		} else {
			saveButton.isEnabled = false
		}
	}
}
