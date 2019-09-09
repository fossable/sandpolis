//****************************************************************************//
//                                                                            //
//                Copyright © 2015 - 2019 Subterranean Security               //
//                                                                            //
//  Licensed under the Apache License, Version 2.0 (the "License");           //
//  you may not use this file except in compliance with the License.          //
//  You may obtain a copy of the License at                                   //
//                                                                            //
//      http://www.apache.org/licenses/LICENSE-2.0                            //
//                                                                            //
//  Unless required by applicable law or agreed to in writing, software       //
//  distributed under the License is distributed on an "AS IS" BASIS,         //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  //
//  See the License for the specific language governing permissions and       //
//  limitations under the License.                                            //
//                                                                            //
//****************************************************************************//
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
