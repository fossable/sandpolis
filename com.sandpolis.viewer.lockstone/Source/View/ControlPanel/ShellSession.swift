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
import SwiftProtobuf

class ShellSession: UIViewController, UITextViewDelegate {

	@IBOutlet weak var shellSelector: UISegmentedControl!
	@IBOutlet weak var textView: UITextView!
	
	var profile: SandpolisProfile!
	
	private var stream: SandpolisStream!
	
	override func viewDidLoad() {
		textView.delegate = self
		
		// Find compatible shells
		SandpolisUtil.connection.shell_list(profile.cvid).whenSuccess { rs in
			DispatchQueue.main.async {
				for shell in rs.rsListShells.listing {
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
	
	@IBAction func onShellChanged(_ sender: Any) {
		if stream != nil {
			stream.close()
		}
		
		switch shellSelector.selectedSegmentIndex {
		case 0:
			stream = SandpolisUtil.connection.shell_session(profile.cvid, self, Net_Shell.pwsh)
		case 1:
			stream = SandpolisUtil.connection.shell_session(profile.cvid, self, Net_Shell.cmd)
		case 2:
			stream = SandpolisUtil.connection.shell_session(profile.cvid, self, Net_Shell.bash)
		default:
			break
		}
	}

	public func onEvent(_ ev: Net_EV_ShellStream) {
		DispatchQueue.main.async {
			self.textView.text = (self.textView.text ?? "") + String(decoding: ev.data, as: UTF8.self)
		}
	}
	
	func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
		
		var ev = Net_MSG.with {
			$0.id = stream.id
			$0.to = profile.cvid
			$0.from = stream.connection.cvid
			$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
				$0.evShellStream = Net_EV_ShellStream.with {
					if text == "'" {
						$0.data = "\n".data(using: .utf8)! // TEMPORARY!!!
					} else {
						$0.data = text.data(using: .utf8)!
					}
				}
			}, typePrefix: "com.sandpolis.plugin.shell")
		}
		stream.connection.send(&ev)
		
		return false
	}
}
