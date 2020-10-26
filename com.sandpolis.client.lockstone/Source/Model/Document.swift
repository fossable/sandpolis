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

class Document {

	private var documents = [String: Document]()

	func document(_ id: String) -> Document {
		if documents[id] == nil {
			documents[id] = Document()
		}
		return documents[id]!
	}

	private var attributes = [String: Attribute]()

	func attribute(_ id: String) -> Attribute {
		if attributes[id] == nil {
			attributes[id] = Attribute("", "")
		}
		return attributes[id]!
	}
}
