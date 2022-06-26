//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit

/// A cell representing a file
class FileCell: UITableViewCell {

	@IBOutlet weak var icon: UIImageView!
	@IBOutlet weak var name: UILabel!
	@IBOutlet weak var size: UILabel!
	@IBOutlet weak var mtime: UILabel!

	func setContent(_ file: Plugin_Filesystem_Msg_FileListlet) {
		// File name
		name.text = file.name

		// File size
		size.text = FormatUtil.formatFileSize(file.size)

		// Modification timestamp
		mtime.text = FormatUtil.formatTimestamp(file.mtime)

		// File icon
		let ext = (file.name as NSString).pathExtension
		if let extIcon = UIImage(named: "extensions/\(ext.lowercased())") {
			icon.image = extIcon
		} else {
			icon.image = UIImage(named: "extensions/blank")
		}
	}
}
