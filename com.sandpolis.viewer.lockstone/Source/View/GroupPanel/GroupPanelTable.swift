//****************************************************************************//
//                                                                            //
//                Copyright © 2015 - 2019 Subterranean Security               //
//                                                                            //
//  Licensed under the Apache License, Version 2.0 (the "License");           //
//  you may not use this file except in compliance with the License.          //
//  You may obtain a copy of the License at                                   //
//                                                                            //
//      http://www.apache.org/licenses/LICENSE-2.0                            //
//                                                                            //
//  Unless required by applicable law or agreed to in writing, software       //
//  distributed under the License is distributed on an "AS IS" BASIS,         //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  //
//  See the License for the specific language governing permissions and       //
//  limitations under the License.                                            //
//                                                                            //
//****************************************************************************//
import UIKit

class GroupPanelTable: UITableViewController {

    @IBOutlet weak var uptime: UILabel!
    @IBOutlet weak var upload: UILabel!
    @IBOutlet weak var download: UILabel!

    var profiles: [SandpolisProfile]!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Compute cumulative download
        download.text = FormatUtil.formatFileSize(profiles.reduce(0) { intermediate, profile in
            return intermediate + profile.downloadTotal
        })

        // Compute cumulative upload
        upload.text = FormatUtil.formatFileSize(profiles.reduce(0) { intermediate, profile in
            return intermediate + profile.uploadTotal
        })

        // Compute cumulative uptime
        let reference = Int64(Date().timeIntervalSince1970) * 1000
        uptime.text = FormatUtil.timeSince(reference - profiles.reduce(0) { intermediate, profile in
            return intermediate + max(reference - profile.startTime, 0)
        })

        tableView.allowsSelection = false
    }
}
