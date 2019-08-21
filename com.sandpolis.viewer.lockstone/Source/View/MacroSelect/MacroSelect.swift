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

class MacroSelect: UITableViewController {

    /// Firebase reference
    private let ref = Database.database().reference(withPath: "\(Auth.auth().currentUser!.uid)/macros")

    var profiles = [SandpolisProfile]()

    var macros = [Macro]()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Synchronize list with Firebase
        ref.observe(.value) { snapshot in
            self.macros = snapshot.children.map { item -> Macro in
                return Macro(item as! DataSnapshot)!
            }.filter { macro in
                // Ensure macro is compatible with every profile
                for profile in self.profiles {
                    switch profile.platform {
                    case .linux:
                        if !macro.linux {
                            return false
                        }
                    case .windows:
                        if !macro.windows {
                            return false
                        }
                    case .macos:
                        if !macro.macos {
                            return false
                        }
                    default:
                        print("Warning: Unknown platform")
                    }
                }
                return true
            }

            self.tableView.reloadData()
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "MacroExecuteSegue",
            let resultView = segue.destination as? MacroResults {

            resultView.profiles = profiles
            resultView.macro = macros[tableView.indexPathForSelectedRow!.row]
        } else {
            fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
        }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if macros.count == 0 {
            let message = UILabel(frame: tableView.bounds)
            message.textAlignment = .center
            message.sizeToFit()
            message.text = "No compatible macros found!"
            message.isEnabled = false
            tableView.backgroundView = message
        } else {
            tableView.backgroundView = nil
        }
        return macros.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "MacroSelectCell") as! MacroSelectCell
        cell.setContent(macros[indexPath.row])
        return cell
    }

}
