//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
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
