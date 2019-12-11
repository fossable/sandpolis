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
import Highlightr

class MacroResultCell: UITableViewCell {

	@IBOutlet weak var textView: UILabel!

	func setContent(_ output: String) {
		textView.text = output

		let highlightr = Highlightr()!
		if let theme = UserDefaults.standard.string(forKey: "terminalTheme") {
			highlightr.setTheme(to: theme)
		} else {
			highlightr.setTheme(to: "zenburn")
		}

		textView.font = highlightr.theme!.codeFont
		textView.backgroundColor = highlightr.theme!.themeBackgroundColor

		// Choose a visible text color for background
		textView.textColor = chooseTextColor(textView.backgroundColor!)
	}

	private func chooseTextColor(_ background: UIColor) -> UIColor {
		var r, g, b, a: CGFloat
		(r, g, b, a) = (0, 0, 0, 0)
		background.getRed(&r, green: &g, blue: &b, alpha: &a)
		return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 0.5 ? UIColor.white : UIColor.black
	}
}
