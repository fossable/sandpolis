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

protocol CollapsibleTableViewHeaderDelegate {
	func toggleSection(_ header: MacroResultHeader, section: Int)
}

class MacroResultHeader: UITableViewHeaderFooterView {

	@IBOutlet weak var platform: UIImageView!
	@IBOutlet weak var hostname: UILabel!
	@IBOutlet weak var status: UILabel!
	@IBOutlet weak var progress: UIActivityIndicatorView!
	@IBOutlet weak var arrow: UILabel!

	var delegate: CollapsibleTableViewHeaderDelegate!
	var section: Int = 0

	func setContent(_ profile: SandpolisProfile) {
		hostname.text = profile.hostname.value
		platform.image = profile.platformIcon
	}

	@objc func didTapHeader(_ gestureRecognizer: UITapGestureRecognizer) {
		guard let cell = gestureRecognizer.view as? MacroResultHeader else {
			return
		}

		delegate?.toggleSection(self, section: cell.section)
	}

	func setCollapsed(_ collapsed: Bool) {
		arrow.rotate(collapsed ? 0.0 : .pi / 2)
	}

	static var nib:UINib {
		return UINib(nibName: identifier, bundle: nil)
	}

	static var identifier: String {
		return "MacroResultHeader"
	}

	override func awakeFromNib() {
		super.awakeFromNib()

		addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didTapHeader)))
	}

}

extension UIView {
	func rotate(_ toValue: CGFloat, duration: CFTimeInterval = 0.2) {
		let animation = CABasicAnimation(keyPath: "transform.rotation")

		animation.toValue = toValue
		animation.duration = duration
		animation.isRemovedOnCompletion = false
		animation.fillMode = CAMediaTimingFillMode.forwards

		self.layer.add(animation, forKey: nil)
	}
}
