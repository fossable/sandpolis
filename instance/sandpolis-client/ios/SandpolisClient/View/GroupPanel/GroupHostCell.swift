//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class GroupHostCell: UITableViewCell {

	@IBOutlet weak var platform: UIImageView!
	@IBOutlet weak var hostname: UILabel!

	func setContent(_ profile: SandpolisProfile) {
		hostname.text = profile.hostname.value as? String
		platform.image = profile.platformIcon
	}
}
