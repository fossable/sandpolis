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

/// A table entry representing a macro
class MacroCell: UITableViewCell {
    
    @IBOutlet weak var name: UILabel!
    @IBOutlet weak var icon_windows: UIImageView!
    @IBOutlet weak var icon_macos: UIImageView!
    @IBOutlet weak var icon_linux: UIImageView!
    
    func setContent(_ macro: Macro) {
        // Macro name
        name.text = macro.name
        
        // Compatibility icons
        icon_windows.isHidden = !macro.windows
        icon_macos.isHidden = !macro.macos
        icon_linux.isHidden = !macro.linux
    }
}
