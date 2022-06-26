//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class GroupActions: UITableViewController {

	var profiles = [SandpolisProfile]()

	private let direct = UserDefaults.standard.string(forKey: "login.type") == "direct"

	override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
		switch indexPath.row {
		case 2, 3:
			return direct ? 0 : 80
		default:
			return 80
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "MacroSelectSegue",
			let dest = segue.destination as? MacroSelect {
			dest.profiles = profiles
		}
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		tableView.deselectRow(at: indexPath, animated: true)

		if indexPath.row == 0 {
			let alert = UIAlertController(title: "Are you sure?", message: "\(profiles.count) hosts will be powered off immediately", preferredStyle: .alert)
			alert.addAction(UIAlertAction(title: "Poweroff", style: .destructive) { _ in
					for profile in self.profiles {
						_ = SandpolisUtil.connection.poweroff(profile.cvid)
					}
					self.navigationController?.popViewController(animated: true)
				})
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

			present(alert, animated: true)
		} else if indexPath.row == 1 {
			let alert = UIAlertController(title: "Are you sure?", message: "\(profiles.count) hosts will be restarted immediately", preferredStyle: .alert)
			alert.addAction(UIAlertAction(title: "Restart", style: .destructive) { _ in
					for profile in self.profiles {
						_ = SandpolisUtil.connection.restart(profile.cvid)
					}
					self.navigationController?.popViewController(animated: true)
				})
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

			present(alert, animated: true)
		} else if indexPath.row == 3 {
			let alert = UIAlertController(title: "Create New Host Group", message: "Enter a group name.", preferredStyle: .alert)
			alert.addTextField { (textField) in
				textField.text = ""
			}
			// TODO
			self.present(alert, animated: true, completion: nil)
		}
	}
}
