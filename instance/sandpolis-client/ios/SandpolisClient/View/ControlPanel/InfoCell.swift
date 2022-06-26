//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class InfoCell: UITableViewCell {

	@IBOutlet weak var title: UILabel!
	@IBOutlet weak var value: UILabel!
	@IBOutlet weak var progress: UIActivityIndicatorView!

	public func setAttribute(_ attribute: STAttribute) {
		title.text = ""
		if let value = attribute.value {
			self.value.text = value as? String
			self.value.isHidden = false
			self.progress.stopAnimating()
		} else {
			self.value.isHidden = true
			self.progress.startAnimating()
		}
	}
}
