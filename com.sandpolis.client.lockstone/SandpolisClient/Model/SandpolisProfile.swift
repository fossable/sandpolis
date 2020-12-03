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

/// Contains agent metadata
class SandpolisProfile {

    lazy var hostname = STAttribute<String>(InstanceOid.hostname.resolve(uuid))
    lazy var osFamily = STAttribute<Core_Foundation_OsType>(InstanceOid.osType.resolve(uuid))
    lazy var online = STAttribute<Bool>(InstanceOid.online.resolve(uuid))
    lazy var ipAddress = STAttribute<String>(InstanceOid.ipAddress.resolve(uuid))
    lazy var ipLocationCity = STAttribute<String>(InstanceOid.ipLocationCity.resolve(uuid))
    lazy var ipLocationContinent = STAttribute<String>(InstanceOid.ipLocationContinent.resolve(uuid))
    lazy var ipLocationCountry = STAttribute<String>(InstanceOid.ipLocationCountry.resolve(uuid))
    lazy var ipLocationCountryCode = STAttribute<String>(InstanceOid.ipLocationCountryCode.resolve(uuid))
    lazy var ipLocationCurrency = STAttribute<String>(InstanceOid.ipLocationCurrency.resolve(uuid))
    lazy var ipLocationDistrict = STAttribute<String>(InstanceOid.ipLocationDistrict.resolve(uuid))
    lazy var ipLocationIsp = STAttribute<String>(InstanceOid.ipLocationIsp.resolve(uuid))
    lazy var ipLocationLatitude = STAttribute<Float32>(InstanceOid.ipLocationLatitude.resolve(uuid))
    lazy var ipLocationLongitude = STAttribute<Float32>(InstanceOid.ipLocationLongitude.resolve(uuid))
    lazy var ipLocationRegion = STAttribute<String>(InstanceOid.ipLocationRegion.resolve(uuid))
    lazy var instanceType = STAttribute<Core_Instance_InstanceType>(InstanceOid.instanceType.resolve(uuid))
    lazy var instanceFlavor = STAttribute<Core_Instance_InstanceFlavor>(InstanceOid.instanceFlavor.resolve(uuid))
    lazy var screenshot = STAttribute<Data>(InstanceOid.screenshot.resolve(uuid))

	func merge(snapshot: Core_Instance_ProtoDocument) {
		merge(oid: "", snapshot: snapshot)
	}

	private func merge(oid: String, snapshot: Core_Instance_ProtoDocument) {
		for attribute in snapshot.attribute {
			switch "\(oid)" {
            case hostname.oid.path:
				hostname.value = attribute.values.first?.string.first
            case osFamily.oid.path:
				osFamily.value = Core_Foundation_OsType.init(rawValue: Int(attribute.values.first?.integer.first ?? 0))
            case online.oid.path:
				online.value = attribute.values.first?.boolean.first
            case ipAddress.oid.path:
				online.value = attribute.values.first?.boolean.first
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
