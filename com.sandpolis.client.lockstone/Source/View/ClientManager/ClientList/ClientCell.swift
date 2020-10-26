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

class ClientCell: UITableViewCell {

	@IBOutlet weak var platform: UIImageView!
	@IBOutlet weak var nameLabel: UILabel!
	@IBOutlet weak var addressLabel: UILabel!

	func setContent(_ profile: SandpolisProfile) {
		nameLabel.text = profile.hostname.value
		addressLabel.text = profile.ipAddress.value

		// Set platform information
		platform.image = profile.platformIcon
	}
}
