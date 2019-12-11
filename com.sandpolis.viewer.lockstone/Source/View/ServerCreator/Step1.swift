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

class Step1: UIViewController {

	@IBOutlet weak var name: UITextField!
	@IBOutlet weak var fqdn: UILabel!
	@IBOutlet weak var nextButton: UIButton!

	var parentController: ServerCreator!

	override func viewDidLoad() {
		super.viewDidLoad()
	}

	@IBAction func cancel(_ sender: Any) {
		dismiss(animated: true, completion: nil)
	}

	@IBAction func next(_ sender: Any) {
		parentController.showStep2()
	}

	@IBAction func nameChange(_ sender: Any) {
		if !name.text!.isEmpty {
			fqdn.text = "\(name.text!).sandpolis.cloud"
		} else {
			fqdn.text = nil
		}
	}
}
