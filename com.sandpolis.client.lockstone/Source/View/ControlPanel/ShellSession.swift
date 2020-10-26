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
import os

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
						case .zsh:
							self.shellSelector.setEnabled(true, forSegmentAt: 3)
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
			os_log("Initial terminal size: %d cols, %d rows", terminal.cols, terminal.rows)
			terminal.stream = SandpolisUtil.connection.shell_session(profile.cvid, self, shell, terminal.cols, terminal.rows)
		}
	}

	public func onEvent(_ ev: Plugin_Shell_Msg_EV_ShellStream) {
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

	private func getShellType() -> Plugin_Shell_Msg_Shell? {
		switch shellSelector.selectedSegmentIndex {
		case 0:
			return Plugin_Shell_Msg_Shell.pwsh
		case 1:
			return Plugin_Shell_Msg_Shell.cmd
		case 2:
			return Plugin_Shell_Msg_Shell.bash
		case 3:
			return Plugin_Shell_Msg_Shell.zsh
		default:
			return nil
		}
	}
}
