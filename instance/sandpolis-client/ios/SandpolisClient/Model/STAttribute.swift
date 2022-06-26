//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import Foundation

class STAttribute {

	/// The attribute OID
	let oid: Oid

	/// The attribute's current value
	var value: Any?

	/// The timestamp associated with the attribute's current value
	var timestamp: Date?

	init(_ parent: STDocument, _ id: String) {
		self.oid = parent.oid.child(id)
	}

	init(_ oid: String) {
		self.oid = Oid(oid)!
	}

	init(_ oid: Oid) {
		self.oid = oid
	}

	func merge(_ snapshot: Core_Instance_ProtoSTObjectUpdate) {
		for (_, change) in snapshot.changed {
			let v = change.value.first!
			switch v.singularType {
			case .bytes?:
				self.value = v.bytes
			case .string?:
				self.value = v.string
			case .integer?:
				self.value = v.integer
			case .long?:
				self.value = v.long
			case .boolean?:
				self.value = v.boolean
			case .double?:
				self.value = v.double
			case .instanceType?:
				self.value = Core_Instance_InstanceType.init(rawValue: Int(v.instanceType))
			case .osType?:
				self.value = Core_Foundation_OsType.init(rawValue: Int(v.osType))
			case nil: break
			}
		}
	}
}
