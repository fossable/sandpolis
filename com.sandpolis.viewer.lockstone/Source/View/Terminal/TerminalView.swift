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
import WebKit

// This source file is part of the https://github.com/ColdGrub1384/Pisth open source project
//
// Copyright (c) 2017 - 2018 Adrian Labbé
// Licensed under Apache License v2.0
//
// See https://raw.githubusercontent.com/ColdGrub1384/Pisth/master/LICENSE for license information

class TerminalView: WKWebView, UIGestureRecognizerDelegate {

	/// Show menu. Called from a gesture recognizer.
	var showMenu: ((UILongPressGestureRecognizer) -> Void)?

	/// Toggle keyboard. Called from a gesture recognizer.
	var toggleKeyboard: (() -> Void)?

	/// The terminal containing the terminal.
	var terminal: TerminalViewController?

	@objc private func showMenu_(_ gestureRecognizer: UILongPressGestureRecognizer) {
		showMenu?(gestureRecognizer)
	}

	@objc private func toggleKeyboard_() {
		toggleKeyboard?()
	}

	private var longPress: UILongPressGestureRecognizer!

	private var tap: UITapGestureRecognizer!

	required init?(coder: NSCoder) {
		super.init(coder: coder)
	}

	override init(frame: CGRect, configuration: WKWebViewConfiguration) {
		super.init(frame: frame, configuration: configuration)

		tap = UITapGestureRecognizer(target: self, action: #selector(toggleKeyboard_))
		addGestureRecognizer(tap)
	}

	override func addGestureRecognizer(_ gestureRecognizer: UIGestureRecognizer) {
		super.addGestureRecognizer(gestureRecognizer)
		gestureRecognizer.delegate = self
	}

	override func becomeFirstResponder() -> Bool {
		return false
	}

	override var canBecomeFirstResponder: Bool {
		return false
	}

	func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {

		if (gestureRecognizer == tap && otherGestureRecognizer == longPress) || (gestureRecognizer == longPress && otherGestureRecognizer == tap) {
			return false
		}

		return true
	}
}
