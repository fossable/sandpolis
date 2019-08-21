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
import Highlightr
import MultiSelectSegmentedControl

class MacroEditor: UIViewController {

    @IBOutlet weak var platformSelector: MultiSelectSegmentedControl!
    @IBOutlet weak var genericView: UIView!
    @IBOutlet weak var languageBar: UILabel!

    var nameField: UITextField = UITextField(frame: CGRect(x: 0, y: 0, width: 210, height: 21))

    let textStorage = CodeAttributedString()
    var highlightr: Highlightr!
    var textView: UITextView!
    var saveButton: UIBarButtonItem!

    /// The macro that is being edited
    var editMacro: Macro!

    var delegate: MacroManagerDelegate!

    override func viewDidLoad() {
        super.viewDidLoad()

        nameField.placeholder = "Enter a macro name"
        nameField.textAlignment = .center
        nameField.borderStyle = .roundedRect
        nameField.addTarget(self, action: #selector(refreshSaveButton), for: .editingChanged)
        navigationItem.titleView = nameField

        saveButton = UIBarButtonItem(title: editMacro == nil ? "Save" : "Update", style: .done, target: self, action: #selector(saveMacro))
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

        if editMacro != nil {
            nameField.text = editMacro.name
            textView.text = editMacro.script

            if editMacro.windows {
                platformSelector.selectedSegmentIndexes.insert(0)
            }
            if editMacro.macos {
                platformSelector.selectedSegmentIndexes.insert(1)
            }
            if editMacro.linux {
                platformSelector.selectedSegmentIndexes.insert(2)
            }
            platformChanged(self)
        } else {
            // Default to linux
            platformSelector.selectedSegmentIndexes.insert(2)
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
        delegate.updateMacro(editMacro, nameField.text!, textView.text!, platformSelector.selectedSegmentTitles.contains("Windows"), platformSelector.selectedSegmentTitles.contains("Linux"), platformSelector.selectedSegmentTitles.contains("macOS"))

        navigationController?.popViewController(animated: true)
    }

    @IBAction func platformChanged(_ sender: Any) {
        let selected = Set(platformSelector.selectedSegmentTitles)

        if selected == ["Linux"] {
            changeLanguage("bash")
        } else if selected == ["macOS"] {
            changeLanguage("bash")
        } else if selected == ["macOS", "Linux"] {
            changeLanguage("bash")
        } else if selected == ["Windows"] {
            changeLanguage("powershell")
        } else {
            changeLanguage("Python is currently unsupported!")
        }

        refreshSaveButton()
    }

    private func changeTheme(_ theme: String) {
        highlightr.setTheme(to: theme)

        textView.backgroundColor = highlightr.theme.themeBackgroundColor
        languageBar.backgroundColor = highlightr.theme.themeBackgroundColor
    }

    private func changeLanguage(_ language: String) {
        textStorage.language = language
        languageBar.text = language
    }

    @objc private func refreshSaveButton() {
        if nameField.text!.count > 0, languageBar.text! == "bash" || languageBar.text! == "powershell" {
            saveButton.isEnabled = true
        } else {
            saveButton.isEnabled = false
        }
    }
}
