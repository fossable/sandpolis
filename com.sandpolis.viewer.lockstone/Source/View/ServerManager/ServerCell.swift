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

class ServerCell: UITableViewCell {

	@IBOutlet weak var nameLabel: UILabel!
	@IBOutlet weak var addressLabel: UILabel!
	@IBOutlet weak var statusLabel: UILabel!
	@IBOutlet weak var locationIcon: UIImageView!
	@IBOutlet weak var progress: UIActivityIndicatorView!

	func setContent(_ server: SandpolisServer) {
		addressLabel.text = server.address
		nameLabel.text = server.name

		// Set IP location
		if let countryCode = server.countryCode {
			if let flag = UIImage(named: "flag/\(countryCode.lowercased())") {
				locationIcon.isHidden = false
				locationIcon.image = flag
			} else {
				locationIcon.isHidden = true
				print("Missing image for country code:", countryCode)
			}
		} else {
			locationIcon.isHidden = true
		}

		// Set online status
		if let online = server.online {
			if online {
				accessoryType = .disclosureIndicator
				statusLabel.isEnabled = false
				statusLabel.text = ""
			} else {
				accessoryType = .none
				statusLabel.isEnabled = true
				statusLabel.text = "offline"
				statusLabel.textColor = UIColor.red
			}
			progress.stopAnimating()
		} else {
			accessoryType = .none
			progress.startAnimating()
		}
	}
}
