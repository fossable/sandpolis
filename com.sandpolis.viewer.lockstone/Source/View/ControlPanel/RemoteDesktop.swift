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

class RemoteDesktop: UIViewController {

	@IBOutlet weak var imageView: UIImageView!

	var profile: SandpolisProfile!

	private var stream: SandpolisStream!

	override func viewDidDisappear(_ animated: Bool) {
		defer {
			stream = nil
		}
		if stream != nil {
			_ = stream.close()
		}
	}

	override func viewWillAppear(_ animated: Bool) {
		stream = SandpolisUtil.connection.remote_desktop(profile.cvid, self)
	}

	public func onEvent(_ ev: Plugin_Desktop_Msg_EV_DesktopStream) {
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
