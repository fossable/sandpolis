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
import NIOSSL

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
				login(address: server.address, username: server.username, password: server.password) { connection in
					if let connection = connection {
						SandpolisUtil.connection = connection
						connection.openProfileStream()
						DispatchQueue.main.async {
							self.performSegue(withIdentifier: "ShowHostSegue", sender: server)
						}
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
			let addServerView = segue.destination as? AddServer {
			addServerView.serverReference = ref.document()
		} else if segue.identifier == "EditServerSegue",
			let addServerView = segue.destination as? AddServer {
			let indexPath = sender as! IndexPath
			addServerView.server = servers[indexPath.row]
			addServerView.serverReference = addServerView.server.reference
		} else if segue.identifier == "ShowHostSegue",
			let mainTab = segue.destination as? MainTabController {
			mainTab.title = (sender as! SandpolisServer).name
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	// Called on disconnection
	@IBAction func unwindToServer(segue: UIStoryboardSegue) {
	}
}
