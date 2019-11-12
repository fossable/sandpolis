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
import Foundation
import UIKit

// This source file is part of the https://github.com/ColdGrub1384/Pisth open source project
//
// Copyright (c) 2017 - 2018 Adrian Labbé
// Licensed under Apache License v2.0
//
// See https://raw.githubusercontent.com/ColdGrub1384/Pisth/master/LICENSE for license information

/// Ansi colors used by terminal.
///
/// If nil is provided for a color, the default color will be used.
public struct AnsiColors {

	/// Black color.
	public var black: String?

	/// Red color.
	public var red: String?

	/// Green color.
	public var green: String?

	/// Yellow color.
	public var yellow: String?

	/// Blue color.
	public var blue: String?

	/// Magenta color.
	public var magenta: String?

	/// Cyan color.
	public var cyan: String?

	/// White color.
	public var white: String?


	/// Bright black color.
	public var brightBlack: String?

	/// Bright red color.
	public var brightRed: String?

	/// Bright green color.
	public var brightGreen: String?

	/// Bright yellow color.
	public var brightYellow: String?

	/// Bright blue color.
	public var brightBlue: String?

	/// Bright magenta color.
	public var brightMagenta: String?

	/// Bright cyan color.
	public var brightCyan: String?

	/// Bright white color.
	public var brightWhite: String?
}

extension UIColor {
	var xtermColor: String {
		let color = CIColor(color: self)
		return "rgba(\(Int(color.red*255)), \(Int(color.green*255)), \(Int(color.blue*255)), \(color.alpha))"
	}
}

open class TerminalTheme {

	/// Keyboard appearance used in terminal.
	///
	/// Default is dark.
	open var keyboardAppearance: UIKeyboardAppearance {
		return .dark
	}

	/// Style used in toolbar in terminal.
	///
	/// Default is black.
	open var toolbarStyle: UIBarStyle {
		return .default
	}

	/// Selection color
	open var selectionColor: UIColor? {
		return nil
	}

	/// Cursor colors.
	open var cursorColor: UIColor? {
		return nil
	}

	/// Default text color.
	open var foregroundColor: UIColor? {
		return nil
	}

	/// Background color.
	open var backgroundColor: UIColor? {
		return nil
	}

	/// ANSI colors.
	open var ansiColors: AnsiColors? {
		return nil
	}

	open var xterm: String {

		var theme: [String: String] = [:]

		if let foregroundColor = foregroundColor {
			theme["foreground"] = foregroundColor.xtermColor
		}

		if let backgroundColor = backgroundColor {
			theme["background"] = backgroundColor.xtermColor
		}

		if let cursorColor = cursorColor {
			theme["cursor"] = cursorColor.xtermColor
		}

		if let selectionColor = selectionColor {
			theme["selection"] = selectionColor.xtermColor
		}

		if self.ansiColors != nil {
			if self.ansiColors?.black != nil {
				theme["black"] = self.ansiColors!.black!
			}

			if self.ansiColors?.red != nil {
				theme["red"] = self.ansiColors!.red!
			}

			if self.ansiColors?.green != nil {
				theme["green"] = self.ansiColors!.green!
			}

			if self.ansiColors?.yellow != nil {
				theme["yellow"] = self.ansiColors!.yellow!
			}

			if self.ansiColors?.blue != nil {
				theme["blue"] = self.ansiColors!.blue!
			}

			if self.ansiColors?.magenta != nil {
				theme["magenta"] = self.ansiColors!.magenta!
			}

			if self.ansiColors?.cyan != nil {
				theme["cyan"] = self.ansiColors!.cyan!
			}

			if self.ansiColors?.white != nil {
				theme["white"] = self.ansiColors!.white!
			}


			if self.ansiColors?.brightBlack != nil {
				theme["brightBlack"] = self.ansiColors!.brightBlack!
			}

			if self.ansiColors?.brightRed != nil {
				theme["brightRed"] = self.ansiColors!.brightRed!
			}

			if self.ansiColors?.brightGreen != nil {
				theme["brightGreen"] = self.ansiColors!.brightGreen!
			}

			if self.ansiColors?.brightYellow != nil {
				theme["brightYellow"] = self.ansiColors!.brightYellow!
			}

			if self.ansiColors?.brightBlue != nil {
				theme["brightBlue"] = self.ansiColors!.blue!
			}

			if self.ansiColors?.brightMagenta != nil {
				theme["brightMagenta"] = self.ansiColors!.brightMagenta!
			}

			if self.ansiColors?.brightCyan != nil {
				theme["brightCyan"] = self.ansiColors!.brightCyan!
			}

			if self.ansiColors?.brightWhite != nil {
				theme["brightWhite"] = self.ansiColors!.brightWhite!
			}
		}

		let xterm = (theme.compactMap { key, val -> String in
			return "\(key)='\(val)'"
		} as Array).joined(separator: ",")

		return "{\(xterm)}"
	}
}
