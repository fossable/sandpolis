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

class About: UIViewController {

	override var preferredStatusBarStyle: UIStatusBarStyle {
		return .lightContent
	}

	@IBAction func openWebsite(_ sender: Any) {
		if let url = URL(string: "https://sandpolis.com") {
			UIApplication.shared.open(url)
		}
	}

	@IBAction func close(_ sender: Any) {
		self.dismiss(animated: true, completion: nil)
	}
}
