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
import Foundation

/// The result of a macro execution on a client
class MacroResult {

	/// Whether the content is collapsed
	var collapsed: Bool

	/// The macro output
	var output: String?

	/// The macro return value
	var returnValue: Int32?

	init() {
		self.collapsed = true
		self.output = nil
		self.returnValue = nil
	}
}
