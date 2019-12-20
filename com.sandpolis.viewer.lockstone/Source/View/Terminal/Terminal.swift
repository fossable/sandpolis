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
import AVFoundation
import SwiftProtobuf

// This source file is part of the https://github.com/ColdGrub1384/Pisth open source project
//
// Copyright (c) 2017 - 2018 Adrian Labbé
// Licensed under Apache License v2.0
//
// See https://raw.githubusercontent.com/ColdGrub1384/Pisth/master/LICENSE for license information

class TerminalViewController: UIViewController, WKNavigationDelegate, WKUIDelegate, UIKeyInput, UITextInput, UITextInputTraits, UIGestureRecognizerDelegate {

	var stream: SandpolisStream!

	var profile: SandpolisProfile!

	/// The button for becoming or resigning first responder.
	var keyboardButton: UIBarButtonItem!

	/// Right bar button items.
	var rightBarButtonItems: [UIBarButtonItem] {
		if keyboardButton == nil {
			if #available(iOS 13.0, *) {
				keyboardButton = UIBarButtonItem(image: UIImage(systemName: "keyboard.chevron.compact.down"), style: .plain, target: self, action: #selector(toggleKeyboard))
			} else {
				keyboardButton = UIBarButtonItem(image: #imageLiteral(resourceName: "hide-keyboard"), style: .plain, target: self, action: #selector(toggleKeyboard))
			}
		}
		let items = [keyboardButton]
		for item in items {
			item?.tintColor = UIApplication.shared.keyWindow?.tintColor
		}
		return (items as? [UIBarButtonItem]) ?? []
	}

	/// Web view used to display content.
	var webView: TerminalView!

	/// Text view with plain output
	var selectionTextView: UITextView!

	/// The theme currently used by the terminal.
	var theme: TerminalTheme = UbuntuTheme()

	/// Ignored notifications name strings.
	/// When a the function linked with a notification listed here, the function will remove the given notification from this array and will return.
	var ignoredNotifications = [Notification.Name]()

	/// The current number of columns
	var cols: Int32 = 0

	/// The current number of rows
	var rows: Int32 = 0

	/// Change terminal size to page size.
	///
	/// - Parameters:
	///     - completion: Function to call after resizing terminal.
	func changeSize(completion: (() -> Void)?) {

		// Get and set columns
		webView.evaluateJavaScript("term.cols") { (cols, error) in

			if let cols = cols as? Int32 {
				self.cols = cols
			}

			// Get and set rows
			self.webView.evaluateJavaScript("term.rows") { (rows, error) in
				if let rows = rows as? Int32 {
					self.rows = rows

					if let stream = self.stream {
						// Inform stream of change
						var ev = Net_MSG.with {
							$0.id = stream.id
							$0.to = self.profile.cvid
							$0.from = stream.connection.cvid
							$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
								$0.evShellStream = Net_EV_ShellStream.with {
									$0.cols = self.cols
									$0.rows = self.rows
								}
							}, typePrefix: "com.sandpolis.plugin.shell")
						}
						stream.connection.send(&ev)
					}

					if let completion = completion {
						completion()
					}
				}
			}
		}
	}

	/// Show plain output and allow selection.
	@objc func selectionMode() {
		_ = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: false, block: { (_) in
			self.selectionTextView.isHidden = false
			self.view.backgroundColor = self.selectionTextView.backgroundColor
			self.selectionTextView.text = ""
			self.webView.isHidden = true
			_ = self.resignFirstResponder()
		})

		_ = Timer.scheduledTimer(withTimeInterval: 1, repeats: false, block: { (_) in
			self.webView.evaluateJavaScript("fit(term); term.selectAll(); term.selectionManager.selectionText", completionHandler: { (result, _) in

				if let result = result as? String {
					self.selectionTextView.text = result
					self.selectionTextView.scrollRangeToVisible(NSRange(location: (self.selectionTextView.text as NSString).length, length: 1))
				}

				self.webView.evaluateJavaScript("term.selectionManager.setSelection(0)", completionHandler: nil)
			})
		})
	}

	/// Hide plain output and disallow selection.
	@objc func insertMode() {
		selectionTextView.isHidden = true
		webView.isHidden = false
		if theme.keyboardAppearance == .dark {
			view.backgroundColor = .black
		} else {
			if #available(iOS 13.0, *) {
				view.backgroundColor = .systemBackground
			} else {
				view.backgroundColor = .white
			}
		}
		_ = becomeFirstResponder()
	}

	/// Send clipboard.
	@objc func pasteText() {
		if isFirstResponder {
			insertText(UIPasteboard.general.string ?? "")
		}
	}

	/// Send text selected in `selectionTextView`.
	@objc func pasteSelection() {
		guard !selectionTextView.isHidden else {
			return
		}

		if let range = selectionTextView.selectedTextRange, let text = selectionTextView.text(in: range) {
			insertText(text)
		}

		insertMode()
	}

	/// Hide or show navigation bar.
	@objc func showNavBar() {
		navigationController?.setNavigationBarHidden(!navigationController!.isNavigationBarHidden, animated: true)
		DispatchQueue.main.asyncAfter(deadline: .now()+0.5) {
			self.fit()
		}
	}

	/// Show or hide keyboard.
	@objc func toggleKeyboard() {

		if isFirstResponder {
			_ = resignFirstResponder()
		} else {
			_ = becomeFirstResponder()
		}
	}

	/// Play bell.
	@objc func bell() {
		AudioServicesPlayAlertSound(1054)
	}

	/// Reload terminal with animation.
	@objc func reload() {

		let view = UIVisualEffectView(frame: webView.frame)

		if keyboardAppearance == .dark {
			view.effect = UIBlurEffect(style: .dark)
		} else {
			view.effect = UIBlurEffect(style: .light)
		}

		view.alpha = 0
		view.tag = 5

		self.view.addSubview(view)

		webView.reload()

		UIView.animate(withDuration: 0.5) {
			view.alpha = 1
		}
	}

	/// Resize and reload `webView`.
	func resizeView(withSize size: CGSize) {
		webView.frame.size = size
		webView.frame.origin = CGPoint(x: 0, y: 0)

		guard !webView.isLoading, webView.url != nil else {
			return
		}

		fit()
	}

	/// Fit the terminal content.
	func fit() {
		webView.evaluateJavaScript("fit(term)", completionHandler: {_, _ in
			self.changeSize(completion: nil)
		})
	}

	/// Called by `NotificationCenter` to inform the theme changed.
	@objc func themeDidChange(_ notification: Notification) {
		guard let theme = notification.object as? TerminalTheme else {
			return
		}

		navigationController?.navigationBar.barStyle = theme.toolbarStyle

		keyboardAppearance = theme.keyboardAppearance
		selectionTextView.keyboardAppearance = keyboardAppearance

		reloadInputViews()

		reload()
	}

	override func paste(_ sender: Any?) {
		self.pasteText()
	}

	override var canBecomeFirstResponder: Bool {
		return (webView != nil)
	}

	override var canResignFirstResponder: Bool {
		return true
	}

	override func resignFirstResponder() -> Bool {
		super.resignFirstResponder()

		webView.evaluateJavaScript("term.setOption('cursorStyle', 'bar')", completionHandler: nil)

		if #available(iOS 13.0, *) {
			//keyboardButton?.image = UIImage(systemName: "keyboard.chevron.compact.down")?.rotate(byDegrees: 180)
		} else {
			keyboardButton?.image = #imageLiteral(resourceName: "show-keyboard")
		}

		return true
	}

	override func becomeFirstResponder() -> Bool {
		super.becomeFirstResponder()

		webView.isUserInteractionEnabled = true

		guard selectionTextView.isHidden else {
			return false
		}

		TerminalViewController.current_ = self
		webView.evaluateJavaScript("term.setOption('cursorStyle', 'block')", completionHandler: nil)

		if #available(iOS 13.0, *) {
			keyboardButton?.image = UIImage(systemName: "keyboard.chevron.compact.down")
		} else {
			keyboardButton?.image = #imageLiteral(resourceName: "hide-keyboard")
		}

		return true
	}

	override func viewDidLoad() {
		super.viewDidLoad()

		edgesForExtendedLayout = []

		// Notifications
		NotificationCenter.default.addObserver(self, selector: #selector(keyboardDidShow), name: UIResponder.keyboardDidShowNotification, object: nil)
		NotificationCenter.default.addObserver(self, selector: #selector(keyboardDidHide), name: UIResponder.keyboardDidHideNotification, object: nil)
		NotificationCenter.default.addObserver(self, selector: #selector(themeDidChange), name: .init("TerminalThemeDidChange"), object: nil)

		// Create WebView
		let config = WKWebViewConfiguration()
		config.mediaTypesRequiringUserActionForPlayback = .video
		webView = TerminalView(frame: CGRect(x: 0, y: 0, width: view.frame.width, height: view.frame.height), configuration: config)
		webView.terminal = self
		webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
		webView.isOpaque = false
		_ = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: false, block: { (_) in
			self.webView.loadFileURL(Bundle.main.url(forResource: "terminal", withExtension: "html")!, allowingReadAccessTo: URL(string:"file:///")!)
		})
		view.addSubview(webView)
		webView.backgroundColor = .clear
		webView.navigationDelegate = self
		webView.uiDelegate = self
		webView.scrollView.isScrollEnabled = false
		// webView.ignoresInvertColors = true
		webView.toggleKeyboard = toggleKeyboard

		// Create selection Textview
		selectionTextView = UITextView(frame: view.frame)
		selectionTextView.isHidden = true
		selectionTextView.font = UIFont(name: "Courier", size: 15)
		selectionTextView.isEditable = false
		selectionTextView.translatesAutoresizingMaskIntoConstraints = false
		view.addSubview(selectionTextView)
		NSLayoutConstraint.activate([
			selectionTextView.leadingAnchor.constraint(equalTo: webView.layoutMarginsGuide.leadingAnchor),
			selectionTextView.trailingAnchor.constraint(equalTo: webView.layoutMarginsGuide.trailingAnchor)
		])
		if #available(iOS 11.0, *) {
			NSLayoutConstraint.activate([
				selectionTextView.topAnchor.constraint(equalToSystemSpacingBelow: webView.layoutMarginsGuide.topAnchor, multiplier: 1.0),
				webView.layoutMarginsGuide.bottomAnchor.constraint(equalToSystemSpacingBelow: selectionTextView.bottomAnchor, multiplier: 1.0)
			])
		}

		keyboardAppearance = theme.keyboardAppearance
		selectionTextView.keyboardAppearance = keyboardAppearance
	}

	override func viewDidAppear(_ animated: Bool) {
		super.viewDidAppear(animated)

		webView.backgroundColor = theme.backgroundColor
		view.backgroundColor = theme.backgroundColor
	}

	override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
		super.viewWillTransition(to: size, with: coordinator)

		let wasFirstResponder = isFirstResponder

		coordinator.animate(alongsideTransition: { (_) in

			if wasFirstResponder {
				_ = self.resignFirstResponder()
			}

		}) { (_) in

			self.reload()

			if wasFirstResponder {
				_ = self.becomeFirstResponder()
			}

		}
	}

	override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
		super.traitCollectionDidChange(previousTraitCollection)

		themeDidChange(Notification(name: Notification.Name(rawValue: "TerminalThemeDidChange"), object: theme, userInfo: nil))
	}

	override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
		if selectionTextView.isHidden {
			return (action == #selector(UIResponder.paste(_:)) || action == #selector(selectionMode) || action == #selector(showNavBar))
		} else {
			return (action == #selector(pasteSelection) || action == #selector(insertMode) || action == #selector(showNavBar))
		}
	}

	/// Resize `webView` when keyboard is shown.
	@objc func keyboardDidShow(_ notification:Notification) {

		guard !ignoredNotifications.contains(notification.name) else {
			if let i = ignoredNotifications.firstIndex(of: notification.name) {
				ignoredNotifications.remove(at: i)
			}
			return
		}

		if let keyboardFrame = (notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue {
			resizeView(withSize: CGSize(width: view.frame.width, height: view.frame.height-keyboardFrame.height))
		}

		if !selectionTextView.isHidden, !selectionTextView.isFirstResponder {
			insertMode()
		}
	}

	/// Resize `webView` when keyboard is hidden.
	@objc func keyboardDidHide(_ notification:Notification) {

		guard !ignoredNotifications.contains(notification.name) else {
			if let i = ignoredNotifications.firstIndex(of: notification.name) {
				ignoredNotifications.remove(at: i)
			}
			return
		}

		guard UIScreen.screens.count == 1 else {
			return
		}

		if webView.frame.size != view.bounds.size {
			webView.frame = view.bounds
			webView.frame.size.height -= 20
			fit()
		}
	}

	/// Write from wireless keyboard.
	///
	/// - Parameters:
	///     - command: Command sent from keyboard.
	@objc func write(fromCommand command: UIKeyCommand) {

		if command.modifierFlags.rawValue == 0 {
			/*switch command.input {
			case UIKeyCommand.inputUpArrow?:
				try? channel.write(Keys.arrowUp)
			case UIKeyCommand.inputDownArrow?:
				try? channel.write(Keys.arrowDown)
			case UIKeyCommand.inputLeftArrow?:
				try? channel.write(Keys.arrowLeft)
			case UIKeyCommand.inputRightArrow?:
				try? channel.write(Keys.arrowRight)
			case UIKeyCommand.inputEscape?:
				try? channel.write(Keys.esc)
			default:
				break
			}*/
		} else if command.modifierFlags == .control { // Send CTRL key
			//try? channel.write(Keys.ctrlKey(from: command.input!))
		}
	}

	func write(_ input: Data) {
		let str = String(decoding: input, as: UTF8.self)
			.replacingOccurrences(of:"\u{2028}", with: "\\u2028")
			.replacingOccurrences(of:"\u{2029}", with: "\\u2029")

		let data = try! JSONSerialization.data(withJSONObject: [str])
		let escaped = NSString(data: data, encoding: String.Encoding.utf8.rawValue)!
		webView.evaluateJavaScript("term.write(\(escaped.substring(with: NSMakeRange(1, escaped.length - 2))))")
	}

	func insertText(_ text: String) {
		var ev = Net_MSG.with {
			$0.id = stream.id
			$0.to = profile.cvid
			$0.from = stream.connection.cvid
			$0.plugin = try! Google_Protobuf_Any(message: Net_ShellMSG.with {
				$0.evShellStream = Net_EV_ShellStream.with {
					$0.data = text.data(using: .utf8)!
				}
			}, typePrefix: "com.sandpolis.plugin.shell")
		}
		stream.connection.send(&ev)
	}

	/// Clear the terminal
	func clear() {
		webView.evaluateJavaScript("term.clear()")
	}

	func deleteBackward() {
		// Send delete char
		// TODO
	}

	var hasText: Bool {
		return true
	}

	var keyboardAppearance: UIKeyboardAppearance = .default

	var autocorrectionType: UITextAutocorrectionType = .no

	var autocapitalizationType: UITextAutocapitalizationType = .none

	var smartQuotesType: UITextSmartQuotesType = .no

	var smartDashesType: UITextSmartDashesType = .no

	func replace(_ range: UITextRange, withText text: String) {
		if !text.isEmpty {
			insertText(text)
		}
	}

	func text(in range: UITextRange) -> String? {
		return nil
	}

	var selectedTextRange: UITextRange?

	var markedTextRange: UITextRange?

	var markedTextStyle: [NSAttributedString.Key : Any]?

	func setMarkedText(_ markedText: String?, selectedRange: NSRange) {
		if let text = markedText, !text.isEmpty {
			insertText(text)
		}
	}

	func unmarkText() {}

	var beginningOfDocument: UITextPosition = .init()

	var endOfDocument: UITextPosition = .init()

	func textRange(from fromPosition: UITextPosition, to toPosition: UITextPosition) -> UITextRange? {
		return nil
	}

	func position(from position: UITextPosition, offset: Int) -> UITextPosition? {
		return nil
	}

	func position(from position: UITextPosition, in direction: UITextLayoutDirection, offset: Int) -> UITextPosition? {
		return nil
	}

	func compare(_ position: UITextPosition, to other: UITextPosition) -> ComparisonResult {
		return .orderedSame
	}

	func offset(from: UITextPosition, to toPosition: UITextPosition) -> Int {
		return 0
	}

	var inputDelegate: UITextInputDelegate?

	var tokenizer: UITextInputTokenizer = UITextInputStringTokenizer()

	func position(within range: UITextRange, farthestIn direction: UITextLayoutDirection) -> UITextPosition? {
		return nil
	}

	func characterRange(byExtending position: UITextPosition, in direction: UITextLayoutDirection) -> UITextRange? {
		return nil
	}

	func baseWritingDirection(for position: UITextPosition, in direction: UITextStorageDirection) -> NSWritingDirection {
		return .leftToRight
	}

	func setBaseWritingDirection(_ writingDirection: NSWritingDirection, for range: UITextRange) {}

	func firstRect(for range: UITextRange) -> CGRect {
		return .zero
	}

	func caretRect(for position: UITextPosition) -> CGRect {
		return .zero
	}

	func selectionRects(for range: UITextRange) -> [UITextSelectionRect] {
		return []
	}

	func closestPosition(to point: CGPoint) -> UITextPosition? {
		return nil
	}

	func closestPosition(to point: CGPoint, within range: UITextRange) -> UITextPosition? {
		return nil
	}

	func characterRange(at point: CGPoint) -> UITextRange? {
		return nil
	}

	func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {

		webView.evaluateJavaScript("term.setOption('cursorBlink', true)")

		webView.evaluateJavaScript("term.setOption('fontSize', 12)")
		selectionTextView.font = selectionTextView.font?.withSize(CGFloat(12))

		webView.evaluateJavaScript("term.setOption('theme', \(theme.xterm))")
		selectionTextView.backgroundColor = theme.backgroundColor
		selectionTextView.textColor = theme.foregroundColor
		webView.backgroundColor = theme.backgroundColor
		view.backgroundColor = theme.backgroundColor

		webView.evaluateJavaScript("fit(term)", completionHandler: {_,_ in
			self.changeSize(completion: nil)

			// Animation
			for view in self.view.subviews {
				if view.tag == 5 {
					UIView.animate(withDuration: 0.5, animations: {
						view.alpha = 0
					})

					_ = Timer.scheduledTimer(withTimeInterval: 0.6, repeats: false, block: { (_) in
						view.removeFromSuperview()
					})
				}
			}
		})
	}

	func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
		if message == "bell" {
			bell()
		} else if message.hasPrefix("changeTitle") {
			title = message
		}
		completionHandler()
	}

	func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
		return true
	}

	/// Returns the current terminal wich is the first responder.
	static var current: TerminalViewController? {
		if current_?.isFirstResponder == true {
			return current_
		} else {
			return nil
		}
	}

	static private var current_: TerminalViewController?
}
