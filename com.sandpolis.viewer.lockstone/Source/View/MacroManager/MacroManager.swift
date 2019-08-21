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
import FirebaseAuth
import FirebaseDatabase

protocol MacroManagerDelegate {
    func updateMacro(_ macro: Macro?, _ name: String, _ script: String, _ windows: Bool, _ linux: Bool, _ macos: Bool)
}

class MacroManager: UITableViewController, MacroManagerDelegate {

    /// Firebase reference
    private let ref = Database.database().reference(withPath: "\(Auth.auth().currentUser!.uid)/macros")

    private var macroList = [Macro]()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Synchronize list with Firebase
        ref.observe(.value) { snapshot in
            self.macroList = snapshot.children.map { item -> Macro in
                return Macro(item as! DataSnapshot)!
            }

            self.tableView.reloadData()
        }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return macroList.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let macro = macroList[indexPath.row]

        let cell = tableView.dequeueReusableCell(withIdentifier: "MacroCell", for: indexPath) as! MacroCell
        cell.setContent(macro)
        return cell
    }

    override func tableView(_ tableView: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath:
            IndexPath) -> UISwipeActionsConfiguration? {
        let delete = UIContextualAction(style: .destructive, title: "Delete") { (action, view, completionHandler) in
            // Delete the macro
            self.ref.child(self.macroList[indexPath.row].name).removeValue()
            completionHandler(true)
        }

        return UISwipeActionsConfiguration(actions: [delete])
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "EditorSegue",
            let editor = segue.destination as? MacroEditor {
            editor.delegate = self

            if tableView.indexPathForSelectedRow != nil {
                editor.editMacro = macroList[tableView.indexPathForSelectedRow!.row]
            }
        } else {
            fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
        }
    }

    func updateMacro(_ macro: Macro?, _ name: String, _ script: String, _ windows: Bool, _ linux: Bool, _ macos: Bool) {
        if let macro = macro {
            ref.child(macro.name).removeValue()
        }

        ref.child(name).setValue([
            "name": name,
            "script": script,
            "windows": windows,
            "linux": linux,
            "macos": macos
            ])
    }
}
