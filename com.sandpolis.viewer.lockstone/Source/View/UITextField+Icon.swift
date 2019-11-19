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
