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
import FirebaseFirestore

class MacroResults: UITableViewController, CollapsibleTableViewHeaderDelegate {

	/// The macro to execute
	var macro: DocumentSnapshot!

	/// The list of target clients
	var profiles = [SandpolisProfile]()

	/// The macro result of the parallel item in profiles
	var results = [MacroResult]()

	override func viewDidLoad() {
		super.viewDidLoad()

		tableView.register(MacroResultHeader.nib, forHeaderFooterViewReuseIdentifier: "MacroResultHeader")

		tableView.rowHeight = UITableView.automaticDimension
		tableView.estimatedRowHeight = 150

		for _ in profiles {
			results.append(MacroResult())
		}
		tableView.reloadData()

		// Modify back button to skip the last view controller
		navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Close", style: .plain, target: self, action: #selector(goBack))
		navigationItem.hidesBackButton = true

		// Process the target shell type
		let shell: Plugin_Shell_Msg_Shell
		switch macro["type"] as! String {
		case "powershell":
			shell = .pwsh
		case "cmd":
			shell = .cmd
		case "bash":
			shell = .bash
		case "zsh":
			shell = .zsh
		default:
			shell = .bash
		}

		// Execute on all profiles
		for profile in profiles {
			let result = results[profiles.firstIndex(where: { $0.cvid == profile.cvid })!]
			SandpolisUtil.connection.execute(profile.cvid, shell, macro["script"] as! String).whenComplete { msg in
				switch msg {
				case .success(let msg as Core_Net_MSG):
					if let rs = try? Plugin_Shell_Msg_RS_Execute.init(serializedData: msg.payload) {
						result.output = rs.result
						result.returnValue = rs.exitCode
						DispatchQueue.main.async {
							self.tableView.reloadData()
						}
					}
				default:
					break
				}
			}
		}
	}

	@objc func goBack() {
		let stack = navigationController!.viewControllers as [UIViewController]
		navigationController?.popToViewController(stack[stack.count - 3], animated: true)
	}

	override func numberOfSections(in tableView: UITableView) -> Int {
		return profiles.count
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return results[section].collapsed ? 0 : 1
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let cell = tableView.dequeueReusableCell(withIdentifier: "MacroResultCell", for: indexPath) as! MacroResultCell
		if let output = results[indexPath.section].output {
			cell.setContent(output)
		}
		return cell
	}

	override func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
		let header = tableView.dequeueReusableHeaderFooterView(withIdentifier: "MacroResultHeader") as? MacroResultHeader ?? MacroResultHeader(reuseIdentifier: "MacroResultHeader")

		if let ret = results[section].returnValue {
			if ret == 0 {
				header.status.text = "Completed"
			} else {
				header.status.text = "The client returned a nonzero exit status: \(ret)"
			}
			header.progress.stopAnimating()
			header.arrow.isHidden = false
		} else {
			header.status.text = "Executing..."
			header.progress.startAnimating()
			header.arrow.isHidden = true
		}

		header.setCollapsed(results[section].collapsed)

		header.section = section
		header.delegate = self
		header.setContent(profiles[section])
		return header
	}

	override func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
		return 50
	}

	override func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
		return 0
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		tableView.deselectRow(at: indexPath, animated: false)
	}

	func toggleSection(_ header: MacroResultHeader, section: Int) {
		if results[section].returnValue != nil {
			let collapsed = !results[section].collapsed

			results[section].collapsed = collapsed
			header.setCollapsed(collapsed)

			tableView.reloadData()
		}
	}
}
