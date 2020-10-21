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
import UIKit

class Overview: UIViewController {

	@IBOutlet weak var flag: UIImageView!
	@IBOutlet weak var hostname: UILabel!
	@IBOutlet weak var platformLogo: UIImageView!
	@IBOutlet weak var platform: UILabel!
	@IBOutlet weak var location: UILabel!
	@IBOutlet weak var screenshot: UIImageView!
	@IBOutlet weak var tableContainer: UIView!
	@IBOutlet weak var screenshotProgress: UIActivityIndicatorView!
	@IBOutlet weak var screenshotHeight: NSLayoutConstraint!

	var profile: SandpolisProfile!

	override func viewDidLoad() {
		super.viewDidLoad()

		// Set screenshot if it already exists
		if let data = profile.screenshot.value, let image = UIImage(data: data) {
			screenshotHeight.constant = min(screenshot.bounds.width, (image.size.height / image.size.width) * screenshot.bounds.width)
			screenshot.image = image
		} else {
			// Trigger refresh otherwise
			refreshScreenshot()
		}

		// Set location
		if let code = profile.ipLocationCountryCode.value {
			flag.image = UIImage(named: "flag/\(code.lowercased())")
			location.text = FormatUtil.formatProfileLocation(city: profile.ipLocationCity.value, region: profile.ipLocationRegion.value, country: profile.ipLocationCountry.value)
		} else {
			flag.isHidden = true
			location.text = profile.ipAddress.value
		}

		// Set hostname
		if let host = profile.hostname.value {
			hostname.text = host
		} else {
			hostname.text = profile.uuid
		}

		// Set platform information
		platformLogo.image = profile.platformIcon
		switch profile.osFamily.value {
		case .linux:
			platform.text = "Linux"
		case .darwin:
			platform.text = "macOS"
		case .windows:
			platform.text = "Windows"
		case .bsd:
			platform.text = "FreeBSD"
		default:
			platform.text = "Unknown OS"
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "EmbeddedTable",
			let embed = segue.destination as? OverviewTable {
			embed.profile = profile
		} else if segue.identifier == "FileManagerSegue",
			let dest = segue.destination as? FileManager {
			dest.profile = profile
		} else if segue.identifier == "MacroSegue",
			let dest = segue.destination as? MacroSelect {
			dest.profiles.append(profile)
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	@IBAction func screenshotTapped(_ sender: Any) {
		refreshScreenshot()
	}

	/// Refresh the screenshot view
	private func refreshScreenshot() {
		screenshotProgress.startAnimating()
		SandpolisUtil.connection.screenshot(profile.cvid).whenComplete { result in
			switch result {
			case .success(let rs as Plugin_Desktop_Msg_RS_Screenshot):
				self.profile.screenshot.value = rs.data
				DispatchQueue.main.async {
					self.screenshotProgress.stopAnimating()
					if let image = UIImage(data: self.profile.screenshot.value!) {
						self.screenshotHeight.constant = min(self.screenshot.bounds.width, (image.size.height / image.size.width) * self.screenshot.bounds.width)
						self.screenshot.image = image
					}
				}
			default:
				DispatchQueue.main.async {
					self.screenshotProgress.stopAnimating()
					self.screenshot.isHidden = true
				}
			}
		}
	}
}
