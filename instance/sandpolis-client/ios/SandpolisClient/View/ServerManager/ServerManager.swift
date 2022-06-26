//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit
import NIOTLS

class ServerManager: UITableViewController {

	private var servers = [SandpolisServer]()

	override func viewDidLoad() {
		super.viewDidLoad()

		// Load table data from user defaults
		servers = (UserDefaults.standard.stringArray(forKey: "servers") ?? []).map(SandpolisServer.init).filter { $0 != nil }.map { $0! }

		self.tableView.reloadData()
		self.refreshServerStates()

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
				login(address: server.address, username: server.username, password: server.getPassword()!) { connection in
					if let connection = connection {
						SandpolisUtil.connection = connection
						_ = connection.sync("/profile")

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
			//self.servers[indexPath.row].reference.delete()
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
			let _ = segue.destination as? AddServer {
		}else if segue.identifier == "EditServerSegue",
			let dest = segue.destination as? AddServer {
			if let index = sender as? IndexPath {
				dest.server = servers[index.row]
			}
		} else if segue.identifier == "ShowHostSegue",
			let dest = segue.destination as? AgentManager {
			if let server = sender as? SandpolisServer {
				dest.server = server
			}
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	// Called on disconnection
	@IBAction func unwindToServer(segue: UIStoryboardSegue) {
	}
}
