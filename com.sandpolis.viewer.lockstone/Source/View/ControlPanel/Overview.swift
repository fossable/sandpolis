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

	var profile: SandpolisProfile!

	override func viewDidLoad() {
		super.viewDidLoad()

		// Set screenshot if it already exists
		if let img = profile.screenshot {
			screenshot.image = UIImage(data: img)
		} else {
			// Trigger refresh otherwise
			refreshScreenshot()
		}

		// Set location
		if let code = profile.location?.countryCode {
			flag.image = UIImage(named: "flag/\(code.lowercased())")
			location.text = FormatUtil.formatProfileLocation(profile)
		} else {
			flag.isHidden = true
			location.text = profile.ipAddress
		}

		// Set hostname
		if let host = profile.hostname {
			hostname.text = host
		} else {
			hostname.text = profile.uuid
		}

		// Set platform information
		platformLogo.image = profile.platformIcon
		switch profile.platform {
		case .linux:
			platform.text = "Linux"
		case .macos:
			platform.text = "macOS"
		case .windows:
			platform.text = "Windows"
		case .freebsd:
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
			case .success(let rs as Net_DesktopMSG):
				self.profile.screenshot = rs.rsScreenshot.data
				DispatchQueue.main.async {
					self.screenshotProgress.stopAnimating()
					self.screenshot.image = UIImage(data: self.profile.screenshot!)
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
