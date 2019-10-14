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

class ServerCreator: UIViewController {

	@IBOutlet weak var s1: UIView!
	@IBOutlet weak var s2: UIView!
	@IBOutlet weak var s3: UIView!
	@IBOutlet weak var s4: UIView!

	var step1: Step1!
	var step2: Step2!
	var step3: Step3!
	var step4: Step4!

	override func viewDidLoad() {
		super.viewDidLoad()
		showStep1()
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "Step1Embed", let dest = segue.destination as? Step1 {
			dest.parentController = self
			step1 = dest
		} else if segue.identifier == "Step2Embed", let dest = segue.destination as? Step2 {
			dest.parentController = self
			step2 = dest
		} else if segue.identifier == "Step3Embed", let dest = segue.destination as? Step3 {
			dest.parentController = self
			step3 = dest
		} else if segue.identifier == "Step4Embed", let dest = segue.destination as? Step4 {
			dest.parentController = self
			step4 = dest
		}
	}

	func showStep1() {
		UIView.animate(withDuration: 2.0) {
			self.s1.alpha = 1.0
			self.s2.alpha = 0.0
			self.s3.alpha = 0.0
			self.s4.alpha = 0.0
		}
	}

	func showStep2() {
		UIView.animate(withDuration: 1.0) {
			self.s1.alpha = 0.0
			self.s2.alpha = 1.0
			self.s3.alpha = 0.0
			self.s4.alpha = 0.0
		}
	}

	func showStep3() {
		UIView.animate(withDuration: 1.0) {
			self.s1.alpha = 0.0
			self.s2.alpha = 0.0
			self.s3.alpha = 1.0
			self.s4.alpha = 0.0
		}
	}

	func showStep4() {
		UIView.animate(withDuration: 1.0) {
			self.s1.alpha = 0.0
			self.s2.alpha = 0.0
			self.s3.alpha = 0.0
			self.s4.alpha = 1.0
		}
	}
}
