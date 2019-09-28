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
import FirebaseFirestore

/// A table entry representing a macro
class MacroCell: UITableViewCell {

	@IBOutlet weak var name: UILabel!
	@IBOutlet weak var type: UILabel!
	@IBOutlet weak var size: UILabel!

	func setContent(_ macro: DocumentSnapshot) {
		// Macro name
		name.text = macro["name"] as? String

		// Macro size
		if let scriptLength = (macro["script"] as? String)?.count {
			size.text = FormatUtil.formatFileSize(Int64(scriptLength))
		} else {
			size.text = "0 bytes"
		}

		// Macro type
		type.text = macro["type"] as? String
	}
}
