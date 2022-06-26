//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

extension UITextField {
	func setLeftIcon(_ image: String) {
		let img = UIImageView(frame: CGRect(x: 10, y: 5, width: 23, height: 23))
		img.image = UIImage(named: image)
		let container = UIView(frame: CGRect(x: 20, y: 0, width: 34, height: 34))
		container.addSubview(img)
		self.leftView = container

		// Just to be sure
		self.leftViewMode = .always
	}
}
