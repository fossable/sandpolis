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
import SwiftEventBus

class ClientList: UITableViewController {

	@IBOutlet weak var footerView: UIView!

	/// Whether multiple hosts can be selected concurrently
	private var selectionMode = false

	/// The current selection if selection mode is active
	private var selection = Set<IndexPath>()

	private var connection: SandpolisConnection!

	override func viewDidLoad() {
		super.viewDidLoad()

		connection = SandpolisUtil.connection

		SwiftEventBus.unregister(self)
		SwiftEventBus.onMainThread(self, name: "profileOnlineEvent") { _ in
			self.tableView.reloadData()
		}
		SwiftEventBus.onMainThread(self, name: "profileOfflineEvent") { _ in
			self.tableView.reloadData()
		}

		tableView.addGestureRecognizer(UILongPressGestureRecognizer(target: self, action: #selector(onLongPress)))
	}

	override func viewWillAppear(_ animated: Bool) {
		super.viewWillAppear(animated)

		exitSelectionMode()
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return connection.profiles.count
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let cell = tableView.dequeueReusableCell(withIdentifier: "HostCell", for: indexPath) as! ClientCell
		cell.setContent(connection.profiles[indexPath.row])
		if selectionMode {
			if selection.contains(indexPath) {
				cell.accessoryType = .checkmark
			} else {
				cell.accessoryType = .none
			}
		} else {
			cell.accessoryType = .disclosureIndicator
		}
		return cell
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		if selectionMode {
			addToSelection(indexPath)
		} else {
			performSegue(withIdentifier: "ShowControlPanelSegue", sender: connection.profiles[indexPath.row])
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "ShowControlPanelSegue",
			let dest = segue.destination as? ControlPanel {

			dest.profile = sender as? SandpolisProfile
		} else if segue.identifier == "ShowGroupControlPanelSegue",
			let dest = segue.destination as? GroupControlPanel {

			dest.profiles = selection.map { indexPath in
				return connection.profiles[indexPath.row]
			}
		} else if segue.identifier == "UnwindLoginSegue" {
			// Nothing to do
		} else if segue.identifier == "UnwindServerSegue" {
			// Nothing to do
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	@IBAction func cancelSelection(_ sender: Any) {
		exitSelectionMode()
	}

	@objc func onLongPress(longPressGestureRecognizer: UILongPressGestureRecognizer) {
		if longPressGestureRecognizer.state == UIGestureRecognizer.State.began {
			let touchPoint = longPressGestureRecognizer.location(in: tableView)
			if let indexPath = tableView.indexPathForRow(at: touchPoint) {
				if !selectionMode {
					enterSelectionMode()
				}
				addToSelection(indexPath)
			}
		}
	}

	private func addToSelection(_ indexPath: IndexPath) {
		if selection.contains(indexPath) {
			selection.remove(indexPath)
		} else {
			selection.insert(indexPath)
		}
		if selection.isEmpty {
			exitSelectionMode()
		}
		tableView.reloadData()
	}

	private func enterSelectionMode() {
		selectionMode = true
		footerView.isHidden = false
		selection.removeAll()
		tableView.reloadData()
	}

	private func exitSelectionMode() {
		selectionMode = false
		footerView.isHidden = true
		selection.removeAll()
		tableView.reloadData()
	}
}
