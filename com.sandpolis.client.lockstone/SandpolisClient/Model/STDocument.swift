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

class STDocument {

    private var attributes = [String: STAttribute<Any>]()

	private var documents = [String: STDocument]()

	func document(_ id: String) -> STDocument {
		if documents[id] == nil {
			documents[id] = STDocument()
		}
		return documents[id]!
	}

	func attribute(_ path: String) -> STAttribute<Any> {
		if attributes[path] == nil {
		//	attributes[path] = STAttribute()
		}
		return attributes[path]!
	}
}
