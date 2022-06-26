//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

class GroupPanelTable: UITableViewController {

	@IBOutlet weak var uptime: UILabel!
	@IBOutlet weak var upload: UILabel!
	@IBOutlet weak var download: UILabel!

	var profiles: [SandpolisProfile]!

	override func viewDidLoad() {
		super.viewDidLoad()

		// Compute cumulative download
		// download.text = FormatUtil.formatFileSize(profiles.reduce(0) { intermediate, profile in
		//	return intermediate + profile.downloadTotal
		// })

		// Compute cumulative upload
		// upload.text = FormatUtil.formatFileSize(profiles.reduce(0) { intermediate, profile in
		// 	return intermediate + profile.uploadTotal
		// })

		// Compute cumulative uptime
		// let reference = Int64(Date().timeIntervalSince1970) * 1000
		// uptime.text = FormatUtil.timeSince(reference - profiles.reduce(0) { intermediate, profile in
		// 	return intermediate + max(reference - profile.startTime, 0)
		// })
	}
}
