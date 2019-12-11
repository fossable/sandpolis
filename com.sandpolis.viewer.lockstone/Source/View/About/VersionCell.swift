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

class VersionCell: UITableViewCell {

	@IBOutlet weak var title: UILabel!
	@IBOutlet weak var version: UILabel!

	func setContent(_ dependency: [String: String]) {
		title.text = dependency["name"]
		if let version = Bundle(identifier: dependency["id"]!)?.infoDictionary?["CFBundleShortVersionString"] as? String {
			self.version.text = version
		}
	}
}
