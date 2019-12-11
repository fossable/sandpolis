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

class InfoCell: UITableViewCell {

	@IBOutlet weak var title: UILabel!
	@IBOutlet weak var value: UILabel!
	@IBOutlet weak var progress: UIActivityIndicatorView!

	public func setAttribute(_ attribute: Attribute) {
		title.text = attribute.title
		if let value = attribute.value {
			self.value.text = value
			self.value.isHidden = false
			self.progress.stopAnimating()
		} else {
			self.value.isHidden = true
			self.progress.startAnimating()
		}
	}
}
