//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class Actions: UITableViewController {

	var profile: SandpolisProfile!

	private let direct = UserDefaults.standard.string(forKey: "login.type") == "direct"

	override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
		switch indexPath.row {
		case 2:
			return direct ? 0 : 80
		default:
			return 80
		}
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		tableView.deselectRow(at: indexPath, animated: true)

		if indexPath.row == 0 {
			let alert = UIAlertController(title: "Are you sure?", message: "The host will need to be powered-on manually", preferredStyle: .alert)
			alert.addAction(UIAlertAction(title: "Poweroff", style: .destructive) { _ in
				_ = SandpolisUtil.connection.poweroff(self.profile.cvid)
				self.navigationController?.popViewController(animated: true)
			})
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

			present(alert, animated: true)
		} else if indexPath.row == 1 {
			let alert = UIAlertController(title: "Are you sure?", message: "The host will not reconnect unless persistence is enabled", preferredStyle: .alert)
			alert.addAction(UIAlertAction(title: "Restart", style: .destructive) { _ in
				_ = SandpolisUtil.connection.restart(self.profile.cvid)
				self.navigationController?.popViewController(animated: true)
			})
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

			present(alert, animated: true)
		} else if indexPath.row == 2 {
			performSegue(withIdentifier: "MacroSelectSegue", sender: self)
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "MacroSelectSegue", let dest = segue.destination as? MacroSelect {
			dest.profiles = [profile]
		}
	}
}
