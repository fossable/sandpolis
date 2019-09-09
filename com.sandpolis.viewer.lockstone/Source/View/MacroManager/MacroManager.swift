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

class MacroManager: UITableViewController {

	/// Firebase reference
	private let ref = Firestore.firestore().collection("/user/\(Auth.auth().currentUser!.uid)/macro")

	private var macroList = [DocumentSnapshot]()

	private var macroListener: ListenerRegistration?

	override func viewWillAppear(_ animated: Bool) {
		super.viewWillAppear(animated)

		// Synchronize table data
		macroListener = ref.addSnapshotListener({ querySnapshot, error in
			guard let macros = querySnapshot?.documents else {
				return
			}

			self.macroList = macros
			self.tableView.reloadData()
		})
	}

	override func viewWillDisappear(_ animated: Bool) {
		super.viewWillDisappear(animated)

		if let listener = macroListener {
			listener.remove()
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
			self.macroList[indexPath.row].reference.delete()
			completionHandler(true)
		}

		return UISwipeActionsConfiguration(actions: [delete])
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "AddSegue",
			let editor = segue.destination as? MacroEditor {

			editor.macroReference = ref.document()
		} else if segue.identifier == "EditSegue",
			let editor = segue.destination as? MacroEditor {

			if tableView.indexPathForSelectedRow != nil {
				editor.macro = macroList[tableView.indexPathForSelectedRow!.row]
				editor.macroReference = editor.macro.reference
			}
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}
}
