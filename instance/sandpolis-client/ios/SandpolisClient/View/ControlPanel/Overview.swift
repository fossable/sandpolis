//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
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
		if let data = profile.screenshot.value as? Data, let image = UIImage(data: data) {
			screenshotHeight.constant = min(screenshot.bounds.width, (image.size.height / image.size.width) * screenshot.bounds.width)
			screenshot.image = image
		} else {
			// Trigger refresh otherwise
			refreshScreenshot()
		}

		// Set location
		if let code = profile.ipLocationCountryCode.value as? String {
			flag.image = UIImage(named: "flag/\(code.lowercased())")
			location.text = FormatUtil.formatProfileLocation(city: profile.ipLocationCity.value as? String, region: profile.ipLocationRegion.value as? String, country: profile.ipLocationCountry.value as? String)
		} else {
			flag.isHidden = true
			location.text = profile.ipAddress.value as? String
		}

		// Set hostname
		if let host = profile.hostname.value as? String {
			hostname.text = host
		} else {
			hostname.text = profile.uuid
		}

		// Set platform information
		platformLogo.image = profile.platformIcon
		switch profile.osFamily.value as? Core_Foundation_OsType {
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
					if let image = UIImage(data: self.profile.screenshot.value as! Data) {
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
