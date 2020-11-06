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
import UIKit
import os

/// Contains client metadata
class SandpolisProfile {

	lazy var hostname = STAttribute<String>("1")
	lazy var osFamily = STAttribute<Core_Foundation_OsType>("9.6.0.24")
	lazy var online = STAttribute<Bool>("9.6.0.28")
	lazy var ipAddress = STAttribute<String>("9.6.0.20")
	lazy var ipLocationCity = STAttribute<String>("9.6.0.13.5.12")
	lazy var ipLocationContinent = STAttribute<String>("9.6.0.13.5.16")
	lazy var ipLocationCountry = STAttribute<String>("9.6.0.13.5.24")
	lazy var ipLocationCountryCode = STAttribute<String>("9.6.0.13.5.20")
	lazy var ipLocationCurrency = STAttribute<String>("9.6.0.13.5.32")
	lazy var ipLocationDisctrict = STAttribute<String>("9.6.0.13.5.36")
	lazy var ipLocationIsp = STAttribute<String>("9.6.0.13.5.40")
	lazy var ipLocationLatitude = STAttribute<Float32>("9.6.0.13.5.44")
	lazy var ipLocationLongitude = STAttribute<Float32>("9.6.0.13.5.48")
	lazy var ipLocationRegion = STAttribute<String>("9.6.0.13.5.64")
	lazy var instanceType = STAttribute<Core_Instance_InstanceType>("9.6.0.4")
	lazy var instanceFlavor = STAttribute<Core_Instance_InstanceFlavor>("9.6.0.8")
	lazy var username = STAttribute<String>("9.6.0.20")
	lazy var userDirectory = STAttribute<String>("9.6.0.20")
	lazy var screenshot = STAttribute<Data>("1")

	func merge(snapshot: Core_Instance_ProtoDocument) {
		merge(oid: "", snapshot: snapshot)
	}
	
	private func merge(oid: String, snapshot: Core_Instance_ProtoDocument) {
		for (tag, value) in snapshot.attribute {
			switch "\(oid).\(tag)" {
			case hostname.oid:
				hostname.value = value.values.first?.string.first
			case osFamily.oid:
				osFamily.value = Core_Foundation_OsType.init(rawValue: Int(value.values.first?.integer.first ?? 0))
			case online.oid:
				online.value = value.values.first?.boolean.first
			case ipAddress.oid:
				online.value = value.values.first?.boolean.first
			default:
				break
			}
		}
	}

	/// The client's UUID
	let uuid: String

	/// The client's CVID
	let cvid: Int32

	/// A list of shells that the client supports
	lazy var shells: [Plugin_Shell_Msg_RS_ListShells.ShellListing]? = {
		do {
			if let rs = try SandpolisUtil.connection.shell_list(cvid).wait() as? Plugin_Shell_Msg_RS_ListShells {
				return rs.listing
			}
		} catch {
			os_log("Failed to query shell compatibility from %d", cvid)
		}
		return nil
	}()

	init(uuid: String, cvid: Int32) {
		self.uuid = uuid
		self.cvid = cvid
	}

	var platformIcon: UIImage? {
		switch osFamily.value {
		case .windows:
			return UIImage(named: "platform/windows")
		case .linux:
			return UIImage(named: "platform/linux")
		case .darwin:
			return UIImage(named: "platform/mac")
		case .bsd:
			return UIImage(named: "platform/freebsd")
		default:
			return nil
		}
	}

	var platformGlyph: UIImage? {
		switch osFamily.value {
		case .windows:
			return UIImage(named: "platform/windows-glyph")
		case .linux:
			return UIImage(named: "platform/linux")
		case .darwin:
			return UIImage(named: "platform/mac-glyph")
		case .bsd:
			return UIImage(named: "platform/freebsd-glyph")
		default:
			return nil
		}
	}
}
