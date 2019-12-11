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

class FormatUtil {

	private static let FILE_UNITS = ["bytes", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"]

	/// Convert the given file size into a user-friendly String.
	///
	/// - Parameter size: The file size in bytes
	/// - Returns: A user-friendly value
	static func formatFileSize(_ size: Int64) -> String {
		guard size > 0 else {
			return "0 bytes"
		}
		guard size != 1 else {
			return "1 byte"
		}

		let i = floor(log(Double(size)) / log(1024))

		// Format number with thousands separator and everything below 1 GiB with no decimal places.
		let numberFormatter = NumberFormatter()
		numberFormatter.maximumFractionDigits = i < 3 ? 0 : 1
		numberFormatter.numberStyle = .decimal

		let numberString = numberFormatter.string(from: NSNumber(value: Double(size) / pow(1024, i))) ?? "Unknown"
		return "\(numberString) \(FILE_UNITS[Int(i)])"
	}

	/// Format the duration of time that has elapsed since the given timestamp.
	///
	/// - Parameter timestamp: The epoch timestamp in milliseconds
	/// - Returns: The duration since the timestamp
	static func timeSince(_ timestamp: Int64) -> String {
		let calendar = Calendar.current

		let unitFlags: Set<Calendar.Component> = [.minute, .hour, .day, .weekOfMonth, .month, .year, .second]
		let components: DateComponents = calendar.dateComponents(unitFlags, from: Date(timeIntervalSince1970: TimeInterval(timestamp / 1000)), to: Date())

		let year = components.year ?? 0
		let month = components.month ?? 0
		let week = components.weekOfMonth ?? 0
		let day = components.day ?? 0
		let hour = components.hour ?? 0
		let minute = components.minute ?? 0
		let second = components.second ?? 0

		switch (year, month, week, day, hour, minute, second) {
		case (let year, _, _, _, _, _, _) where year >= 2: return "\(year) years"
		case (let year, _, _, _, _, _, _) where year == 1: return "1 year"
		case (_, let month, _, _, _, _, _) where month >= 2: return "\(month) months"
		case (_, let month, _, _, _, _, _) where month == 1: return "1 month"
		case (_, _, let week, _, _, _, _) where week >= 2: return "\(week) weeks"
		case (_, _, let week, _, _, _, _) where week == 1: return "1 week"
		case (_, _, _, let day, _, _, _) where day >= 2: return "\(day) days"
		case (_, _, _, let day, _, _, _) where day == 1: return "1 day"
		case (_, _, _, _, let hour, _, _) where hour >= 2: return "\(hour) hours"
		case (_, _, _, _, let hour, _, _) where hour == 1: return "1 hour"
		case (_, _, _, _, _, let minute, _) where minute >= 2: return "\(minute) minutes"
		case (_, _, _, _, _, let minute, _) where minute == 1: return "1 minute"
		case (_, _, _, _, _, _, let second) where second >= 2: return "\(second) seconds"
		default: return "1 second"
		}
	}

	/// Format the epoch timestamp.
	///
	/// - Parameters:
	///   - timestamp: The timestamp in milliseconds
	///   - format: The date format
	/// - Returns: A user-friendly date string
	static func formatTimestamp(_ timestamp: Int64, format: String = "yyyy-MM-dd HH:mm:ss") -> String {
		let date = Date(timeIntervalSince1970: TimeInterval(timestamp / 1000))
		return formatDateInTimezone(date, TimeZone.current, format: format)
	}

	/// Format the date in the given timezone.
	///
	/// - Parameters:
	///   - date: The target date
	///   - timezone: The timezone
	///   - format: The date format
	/// - Returns: A user-friendly date string
	static func formatDateInTimezone(_ date: Date, _ timezone: TimeZone, format: String = "yyyy-MM-dd HH:mm:ss") -> String {
		let formatter = DateFormatter()
		formatter.dateFormat = format
		formatter.timeZone = timezone
		return formatter.string(from: date)
	}

	/// Format the profile's location information.
	///
	/// - Parameter profile: The given profile
	/// - Returns: A user-friendly location string
	static func formatProfileLocation(_ profile: SandpolisProfile) -> String {
		if let city = profile.location?.city, let region = profile.location?.region, let country = profile.location?.country {
			return "\(city), \(region) (\(country))"
		}
		if let region = profile.location?.region, let country = profile.location?.country {
			return "\(region), \(country)"
		}
		if let country = profile.location?.country {
			return country
		}

		return "Unknown Location"
	}
}
