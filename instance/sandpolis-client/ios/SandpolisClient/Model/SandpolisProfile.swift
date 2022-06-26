//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import Foundation
import UIKit
import os

/// Contains agent metadata
class SandpolisProfile {

	lazy var hostname = STAttribute("/profile/\(uuid)/agent/hostname")
	lazy var osFamily = STAttribute("/profile/\(uuid)/agent/os_type")
	lazy var online = STAttribute("/profile/\(uuid)/agent/status")
	lazy var ipAddress = STAttribute("/profile/\(uuid)/ip_address")
	lazy var ipLocationCity = STAttribute("/profile/\(uuid)/agent/ip_location/city")
	lazy var ipLocationContinent = STAttribute("/profile/\(uuid)/agent/ip_location/continent")
	lazy var ipLocationCountry = STAttribute("/profile/\(uuid)/agent/ip_location/country")
	lazy var ipLocationCountryCode = STAttribute("/profile/\(uuid)/agent/ip_location/country_code")
	lazy var ipLocationCurrency = STAttribute("/profile/\(uuid)/agent/ip_location/currency")
	lazy var ipLocationDistrict = STAttribute("/profile/\(uuid)/agent/ip_location/district")
	lazy var ipLocationIsp = STAttribute("/profile/\(uuid)/agent/ip_location/isp")
	lazy var ipLocationLatitude = STAttribute("/profile/\(uuid)/agent/ip_location/latitude")
	lazy var ipLocationLongitude = STAttribute("/profile/\(uuid)/agent/ip_location/longitude")
	lazy var ipLocationRegion = STAttribute("/profile/\(uuid)/agent/ip_location/region")
	lazy var instanceType = STAttribute("/profile/\(uuid)/instance_type")
	lazy var instanceFlavor = STAttribute("/profile/\(uuid)/instance_flavor")
	lazy var screenshot = STAttribute(Oid("org.s7s.plugin.desktop", "/profile/\(uuid)/screenshot"))

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
		switch osFamily.value as? Core_Foundation_OsType {
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
		switch osFamily.value as? Core_Foundation_OsType {
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
