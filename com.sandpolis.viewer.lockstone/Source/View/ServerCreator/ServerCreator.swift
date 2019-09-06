import UIKit

class ServerCreator: UIViewController {

    @IBOutlet weak var step1: UIView!
    @IBOutlet weak var step2: UIView!
    @IBOutlet weak var step3: UIView!

    override func viewDidLoad() {
        super.viewDidLoad()
		showStep1()
    }
	
	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "Step1Embed", let dest = segue.destination as? Step1 {
			dest.parentController = self
		} else if segue.identifier == "Step2Embed", let dest = segue.destination as? Step2 {
			dest.parentController = self
		} else if segue.identifier == "Step3Embed", let dest = segue.destination as? Step3 {
			dest.parentController = self
		}
	}

	func showStep1() {
		UIView.animate(withDuration: 2.0) {
			self.step1.alpha = 1.0
			self.step2.alpha = 0.0
			self.step3.alpha = 0.0
		}
	}
	
	func showStep2() {
		UIView.animate(withDuration: 1.0) {
			self.step1.alpha = 0.0
			self.step2.alpha = 1.0
			self.step3.alpha = 0.0
		}
	}
	
	func showStep3() {
		UIView.animate(withDuration: 1.0) {
			self.step1.alpha = 0.0
			self.step2.alpha = 0.0
			self.step3.alpha = 1.0
		}
	}
}
