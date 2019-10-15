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
import Highlightr

class ShellSession: UIViewController {
	@IBOutlet weak var shellSelector: UISegmentedControl!
	@IBOutlet weak var textView: UITextView!
	
	var profile: SandpolisProfile!
	
	private var stream: SandpolisStream!
	
	override func viewDidLoad() {
		stream = SandpolisUtil.connection.shell_session(profile.cvid, self, Net_Shell.bash)
	}
	
	public func onEvent(_ ev: Net_EV_ShellStream) {
		DispatchQueue.main.async {
			self.textView.text = (self.textView.text ?? "") + String(decoding: ev.data, as: UTF8.self)
		}
	}
}
