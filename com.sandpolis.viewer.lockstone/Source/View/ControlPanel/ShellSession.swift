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

class ShellSession: UIViewController {

	@IBOutlet weak var shellSelector: UISegmentedControl!

	@IBOutlet weak var terminalContainer: UIView!

	private var terminal: TerminalViewController!

	var profile: SandpolisProfile!

	override func viewDidLoad() {
		super.viewDidLoad()
		
		DispatchQueue.global().async(qos: .userInitiated) {
			if let shells = self.profile.shells {
				DispatchQueue.main.async {
					for shell in shells {
						switch shell.type {
						case .bash:
							self.shellSelector.setEnabled(true, forSegmentAt: 2)
						case .pwsh:
							self.shellSelector.setEnabled(true, forSegmentAt: 0)
						case .cmd:
							self.shellSelector.setEnabled(true, forSegmentAt: 1)
						default:
							break
						}
					}
				}
			}
		}
	}

	@IBAction func onShellChanged(_ sender: Any) {
		if terminalContainer.isHidden {
			terminalContainer.isHidden = false
		}
		if terminal.stream != nil {
			terminal.stream.close()
		}
		terminal.clear()
		if let shell = getShellType() {
			terminal.stream = SandpolisUtil.connection.shell_session(profile.cvid, self, shell)
		}
	}

	public func onEvent(_ ev: Net_EV_ShellStream) {
		DispatchQueue.main.async {
			self.terminal.write(ev.data)
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "ShellEmbed" {
			terminal = segue.destination as? TerminalViewController
			terminal.profile = profile
		}
	}
	
	private func getShellType() -> Net_Shell? {
		switch shellSelector.selectedSegmentIndex {
		case 0:
			return Net_Shell.pwsh
		case 1:
			return Net_Shell.cmd
		case 2:
			return Net_Shell.bash
		default:
			return nil
		}
	}
}
