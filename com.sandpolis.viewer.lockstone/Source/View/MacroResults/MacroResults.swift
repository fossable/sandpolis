/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
import UIKit

class MacroResults: UITableViewController, CollapsibleTableViewHeaderDelegate {

	/// The macro to execute
	var macro: Macro!

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

		// Execute on all profiles
		for profile in profiles {
			let result = results[profiles.firstIndex(where: { $0.cvid == profile.cvid })!]
			SandpolisUtil.execute(profile.cvid, macro.script).whenSuccess { rs in
				result.output = rs.rsExecute.result
				result.returnValue = rs.rsExecute.exitCode
				DispatchQueue.main.async {
					self.tableView.reloadData()
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
