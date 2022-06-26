//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class AgentCell: UITableViewCell {

	@IBOutlet weak var platform: UIImageView!
	@IBOutlet weak var nameLabel: UILabel!
	@IBOutlet weak var addressLabel: UILabel!

	func setContent(_ profile: SandpolisProfile) {
		nameLabel.text = profile.hostname.value as? String
		addressLabel.text = profile.ipAddress.value as? String

		// Set platform information
		platform.image = profile.platformIcon
	}
}
