//
//  Step1.swift
//  Sandpolis
//
//  Created by Tyler Cook on 8/30/19.
//

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
			fqdn.text = "\(name.text!).sandpolis.com"
		} else {
			fqdn.text = nil
		}
    }
}
