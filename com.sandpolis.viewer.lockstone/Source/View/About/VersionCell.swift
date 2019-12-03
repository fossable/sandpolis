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

class VersionCell: UITableViewCell {

	@IBOutlet weak var title: UILabel!
	@IBOutlet weak var version: UILabel!

	func setContent(_ dependency: [String: String]) {
		title.text = dependency["name"]
		if let version = Bundle(identifier: dependency["id"]!)?.infoDictionary?["CFBundleShortVersionString"] as? String {
			self.version.text = version
		}
	}
}
