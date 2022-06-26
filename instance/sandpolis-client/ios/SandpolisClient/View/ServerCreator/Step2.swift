//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class Step2: UIViewController {

	@IBOutlet weak var username: UITextField!
	@IBOutlet weak var password: UITextField!
	@IBOutlet weak var nextButton: UIButton!

	var parentController: ServerCreator!

	override func viewDidLoad() {
		super.viewDidLoad()
	}

	@IBAction func back(_ sender: Any) {
		parentController.showStep1()
	}

	@IBAction func next(_ sender: Any) {
		parentController.showStep3()
	}
}
