//
//  Step2.swift
//  Sandpolis
//
//  Created by Tyler Cook on 8/30/19.
//

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
