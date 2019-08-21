/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
import UIKit

/// A cell representing a file
class FileCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var name: UILabel!
    @IBOutlet weak var size: UILabel!
    @IBOutlet weak var mtime: UILabel!

    func setContent(_ file: Net_FileListlet) {
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
