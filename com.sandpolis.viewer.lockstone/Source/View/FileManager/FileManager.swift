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

/// The available file sorting schemes
enum SortType: String, CaseIterable {
	case size_asc = "Size Ascending", size_dsc = "Size Descending"
	case time_asc = "Timestamp Ascending", time_dsc = "Timestamp Descending"
	case name_asc = "Name Ascending", name_dsc = "Name Descending"
}

class FileManager: UITableViewController {

	/// A navigation bar containing the current path
	@IBOutlet weak var pathBar: UINavigationItem!
	@IBOutlet weak var sortButton: UIBarButtonItem!

	var profile: SandpolisProfile!

	private var pathLabel: UILabel!

	/// The browser's sorting scheme
	private var sort: SortType = .name_asc

	/// The current file listing
	private var files = [Net_FileListlet]()

	/// Whether the browser is currently at the root directory
	private var root: Bool = false

	/// The current path
	private var path: URL = URL(fileURLWithPath: "/") {
		willSet(new) {
			root = new.path == "/" || new.path == "/C:"
			pathLabel.text = new.path
		}
	}

	override func viewDidLoad() {
		super.viewDidLoad()

		pathLabel = UILabel()
		pathLabel.textAlignment = .left
		pathLabel.lineBreakMode = .byTruncatingHead
		pathLabel.translatesAutoresizingMaskIntoConstraints = false
		pathBar.titleView = pathLabel

		if let home = profile.userhome {
			path = URL(fileURLWithPath: home)
		} else {
			// TODO
			path = URL(fileURLWithPath: ".")
		}

		requestListing()
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return files.count + (root ? 0 : 1)
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		if !root && indexPath.row == 0 {
			return tableView.dequeueReusableCell(withIdentifier: "FolderUpCell", for: indexPath) as! FolderUpCell
		}

		let file = files[indexPath.row - (root ? 0 : 1)]
		if file.directory {
			let cell = tableView.dequeueReusableCell(withIdentifier: "FolderCell", for: indexPath) as! FolderCell
			cell.setContent(file)
			return cell
		} else {
			let cell = tableView.dequeueReusableCell(withIdentifier: "FileCell", for: indexPath) as! FileCell
			cell.setContent(file)
			return cell
		}
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		if !root && indexPath.row == 0 {

			// Move up
			path.deleteLastPathComponent()
			requestListing()

		} else {

			// Move down
			let file = files[indexPath.row - (root ? 0 : 1)]
			if file.directory {
				path.appendPathComponent(file.name)
				requestListing()
			}
		}
	}

	override func tableView(_ tableView: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath:
			IndexPath) -> UISwipeActionsConfiguration? {
		if !root && indexPath.row == 0 {
			return UISwipeActionsConfiguration()
		}

		let delete = UIContextualAction(style: .destructive, title: "Delete") { (action, view, completionHandler) in
			SandpolisUtil.connection.fm_delete(self.profile.cvid, self.path.appendingPathComponent(self.files[indexPath.row - (self.root ? 0 : 1)].name).path)
			completionHandler(true)
		}

		let config = UISwipeActionsConfiguration(actions: [delete])
		config.performsFirstActionWithFullSwipe = false
		return config
	}

	@IBAction func sort(_ sender: Any) {
		let alert = UIAlertController(title: "Sort Criteria", message: nil, preferredStyle: .actionSheet)
		alert.popoverPresentationController?.barButtonItem = sortButton

		SortType.allCases.forEach { st in
			alert.addAction(UIAlertAction(title: st.rawValue, style: .default) { action in
				self.sort = st
				self.sortFiles()
				self.tableView.reloadData()
			})
		}

		present(alert, animated: true, completion: nil)
	}

	/// Request a new listing for the current directory
	private func requestListing() {
		SandpolisUtil.connection.fm_list(profile.cvid, path.path, mtimes: true, sizes: true).whenSuccess { rs in
			self.loadListing(rs)

			DispatchQueue.main.async {
				self.tableView.reloadData()
			}
		}
	}

	/// Load the file listing from the given message
	private func loadListing(_ rs: Net_FilesysMessage) {
		files = rs.rsFileListing.listing

		sortFiles()
	}

	/// Sort the current file listing without updating the tableview
	private func sortFiles() {
		// Directories always first
		var sorted = sortFiles(files.filter {
			$0.directory
		}, sort)

		// Files last
		sorted.append(contentsOf: sortFiles(files.filter {
			!$0.directory
		}, sort))

		files = sorted
	}

	/// Sort the given file list according to the given sort type.
	///
	/// - Parameters:
	///   - list: The file list
	///   - type: The sort type
	/// - Returns: A newly sorted list
	private func sortFiles(_ list: [Net_FileListlet], _ type: SortType) -> [Net_FileListlet] {
		switch type {
		case .size_asc:
			return list.sorted {
				$0.size < $1.size
			}
		case .size_dsc:
			return list.sorted {
				$0.size > $1.size
			}
		case .time_asc:
			return list.sorted {
				$0.mtime < $1.mtime
			}
		case .time_dsc:
			return list.sorted {
				$0.mtime > $1.mtime
			}
		case .name_asc:
			return list.sorted {
				$0.name < $1.name
			}
		case .name_dsc:
			return list.sorted {
				$0.name > $1.name
			}
		}
	}
}
