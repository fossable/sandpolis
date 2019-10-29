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

class RemoteDesktop: UIViewController {

	@IBOutlet weak var imageView: UIImageView!

	var profile: SandpolisProfile!

	private var stream: SandpolisStream!

	override func viewDidDisappear(_ animated: Bool) {
		defer {
			stream = nil
		}
		if stream != nil {
			stream.close()
		}
	}

	override func viewWillAppear(_ animated: Bool) {
		stream = SandpolisUtil.connection.remote_desktop(profile.cvid, self)
	}

	public func onEvent(_ ev: Net_EV_DesktopStream) {
		var rect: CGRect
		switch ev.data! {
		case .dirtyBlock:
			rect = CGRect(x: 0, y: 0, width: 16, height: 16)
		case .dirtyRect:
			rect = CGRect(x: Int(ev.dirtyRect.x), y: Int(ev.dirtyRect.y), width: Int(ev.dirtyRect.w), height: ev.dirtyRect.data.count / Int(ev.dirtyRect.w))
		}

		// TODO draw block
	}
}
