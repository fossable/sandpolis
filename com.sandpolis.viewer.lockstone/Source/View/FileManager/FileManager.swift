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
	private var files = [Plugin_Filesys_Msg_FileListlet]()

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

		if let home = profile.userDirectory.value {
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

		// Cancel
		alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))

		present(alert, animated: true)
	}

	/// Request a new listing for the current directory
	private func requestListing() {
		SandpolisUtil.connection.fm_list(profile.cvid, path.path, mtimes: true, sizes: true).whenComplete { result in
			switch result {
			case .success(let rs as Plugin_Filesys_Msg_RS_FileListing):
				self.loadListing(rs)

				DispatchQueue.main.async {
					self.tableView.reloadData()
				}
			default:
				break
			}
		}
	}

	/// Load the file listing from the given message
	private func loadListing(_ rs: Plugin_Filesys_Msg_RS_FileListing) {
		files = rs.listing

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
	private func sortFiles(_ list: [Plugin_Filesys_Msg_FileListlet], _ type: SortType) -> [Plugin_Filesys_Msg_FileListlet] {
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
