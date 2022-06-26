//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import Foundation
import os

class STDocument {

	/// The document OID
	let oid: Oid

	private var attributes = [String: STAttribute]()

	private var documents = [String: STDocument]()

	func document(_ id: String) -> STDocument {
		if documents[id] == nil {
			documents[id] = STDocument(self, id)
		}
		return documents[id]!
	}

	func attribute(_ id: String) -> STAttribute {
		if attributes[id] == nil {
			attributes[id] = STAttribute(self, id)
		}
		return attributes[id]!
	}

	func attribute(_ oid: Oid) -> STAttribute {

		if(oid.path.count - self.oid.path.count == 1) {
			return attribute(oid.path.last!)
		}

		var document = self
		for i in self.oid.path.count...(oid.path.count - 1) {
			document = document.document(oid.path[i])
		}

		return document.attribute(oid.path.last!)
	}

	init(_ parent: STDocument?, _ id: String) {
		if let parent = parent {
			self.oid = parent.oid.child(id)
		} else {
			self.oid = Oid(id)!
		}
	}

	func merge(_ snapshot: Core_Instance_ProtoSTObjectUpdate) {
		for (path, change) in snapshot.changed {
			os_log("Merging oid: %s", path)
			if let oid = Oid(path) {
				attribute(oid).merge(Core_Instance_ProtoSTObjectUpdate.with {
					$0.changed[path] = change
				})
			}
		}
	}
}
