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
import NIOTLS

class ServerManager: UITableViewController {

	/// Firebase reference
	private let ref = Firestore.firestore().collection("/user/\(Auth.auth().currentUser!.uid)/server")

	private var servers = [SandpolisServer]()

	private var refListener: ListenerRegistration!

	override func viewDidLoad() {
		super.viewDidLoad()

		// Synchronize table data
		refListener = ref.addSnapshotListener({ querySnapshot, error in
			guard let servers = querySnapshot?.documents else {
				return
			}

			self.servers = servers.map { server -> SandpolisServer in
				return SandpolisServer(server)
			}
			self.tableView.reloadData()
			self.refreshServerStates()
		})

		// Setup refresh control
		refreshControl = UIRefreshControl()
		refreshControl?.addTarget(self, action: #selector(refreshTable), for: .valueChanged)
	}

	@objc func refreshTable() {
		// Spawn synchronous connection attempts
		DispatchQueue.global(qos: .utility).async {
			for server in self.servers {
				server.online = SandpolisUtil.testConnect(server.address, 8768)
			}
			DispatchQueue.main.async {
				self.tableView.reloadData()
				self.refreshControl?.endRefreshing()
			}
		}
	}

	/// Attempt to connect to each server in the list
	func refreshServerStates() {

		// Spawn concurrent connection attempts
		for server in servers {
			DispatchQueue.global(qos: .utility).async {
				server.online = SandpolisUtil.testConnect(server.address, 8768)
				DispatchQueue.main.async {
					self.tableView.reloadData()
				}
			}
		}
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return servers.count
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let server = servers[indexPath.row]
		let cell = tableView.dequeueReusableCell(withIdentifier: "ServerCell", for: indexPath) as! ServerCell
		cell.setContent(server)
		return cell
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		let server = servers[indexPath.row]
		if let online = server.online {
			if online {
				tableView.allowsSelection = false
				login(address: server.address, username: server.username, password: server.password) { connection in
					if let connection = connection {
						SandpolisUtil.connection = connection
						//connection.openProfileStream()
						DispatchQueue.main.async {
							self.performSegue(withIdentifier: "ShowHostSegue", sender: server)
						}
					}
					DispatchQueue.main.async {
						tableView.allowsSelection = true
					}
				}
			} else {
				server.online = nil
				self.tableView.reloadData()

				// Retry connection probe
				DispatchQueue.global(qos: .utility).async {
					server.online = SandpolisUtil.testConnect(server.address, 8768)
					DispatchQueue.main.async {
						self.tableView.reloadData()
					}
				}
			}
		}
	}

	override func tableView(_ tableView: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath:
			IndexPath) -> UISwipeActionsConfiguration? {
		let delete = UIContextualAction(style: .destructive, title: "Delete") { action, view, completion in
			self.servers[indexPath.row].reference.delete()
			completion(true)
		}
		let edit = UIContextualAction(style: .normal, title: "Edit") { action, view, completion in
			self.performSegue(withIdentifier: "EditServerSegue", sender: indexPath)
			completion(true)
		}
		let config = UISwipeActionsConfiguration(actions: [delete, edit])
		config.performsFirstActionWithFullSwipe = false
		return config
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "AddServerSegue",
			let dest = segue.destination as? AddServer {
			dest.serverReference = ref.document()
		} else if segue.identifier == "EditServerSegue",
			let dest = segue.destination as? AddServer {
			if let index = sender as? IndexPath {
				dest.server = servers[index.row]
				dest.serverReference = dest.server.reference
			}
		} else if segue.identifier == "ShowHostSegue",
			let dest = segue.destination as? ClientManager {
			if let server = sender as? SandpolisServer {
				dest.loginType = .cloud(server)
			}
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	// Called on disconnection
	@IBAction func unwindToServer(segue: UIStoryboardSegue) {
	}
}
