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
import Highlightr

class MacroResultCell: UITableViewCell {

    @IBOutlet weak var textView: UILabel!

    func setContent(_ output: String) {
        textView.text = output

        let highlightr = Highlightr()!
        if let theme = UserDefaults.standard.string(forKey: "terminalTheme") {
            highlightr.setTheme(to: theme)
        } else {
            highlightr.setTheme(to: "zenburn")
        }

        textView.font = highlightr.theme!.codeFont
        textView.backgroundColor = highlightr.theme!.themeBackgroundColor
        
        // Choose a visible text color for background
        textView.textColor = chooseTextColor(textView.backgroundColor!)
    }

    private func chooseTextColor(_ background: UIColor) -> UIColor {
        var r, g, b, a: CGFloat
        (r, g, b, a) = (0, 0, 0, 0)
        background.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 0.5 ? UIColor.white : UIColor.black
    }
}
