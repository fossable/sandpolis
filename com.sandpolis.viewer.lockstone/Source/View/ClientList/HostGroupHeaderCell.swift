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

class HostGroupHeaderCell: UITableViewCell {

	@IBOutlet weak var groupNameLabel: UILabel!
	@IBOutlet weak var viewButton: UIButton!
	@IBOutlet weak var deleteButton: UIButton!

	var hostList: ClientList!
	var hostGroup: HostGroup!

	@IBAction func viewButtonPressed(_ sender: Any) {
		hostList.viewHostGroup(hostGroup: hostGroup)
	}

	@IBAction func deleteButtonPressed(_ sender: Any) {
		hostList.deleteHostGroup(hostGroup: hostGroup)
	}

	func setContent(hostList: ClientList, hostGroup: HostGroup, defaultGroup: Bool) {
		self.hostList = hostList
		self.hostGroup = hostGroup
		groupNameLabel.text = hostGroup.groupName
		if defaultGroup {
			viewButton.isHidden = true
			deleteButton.isHidden = true
		}
	}
}
