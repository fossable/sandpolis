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
import Firebase
import FirebaseAuth
import GoogleSignIn
import Highlightr

class Settings: UITableViewController {

    @IBOutlet weak var hostView: UISegmentedControl!

    override func viewDidLoad() {
        super.viewDidLoad()
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        if indexPath.row == 1 {
            let alert = UIAlertController(title: "Terminal Themes", message: nil, preferredStyle: .actionSheet)

            for theme in Highlightr()!.availableThemes(){
                alert.addAction(UIAlertAction(title: theme, style: .default) { action in
                    UserDefaults.standard.set(theme, forKey: "terminalTheme")
                })
            }

            present(alert, animated: true, completion: nil)
        } else if indexPath.row == 3 {
            do {
                try Auth.auth().signOut()
            } catch let signOutError as NSError {
                print ("Error signing out: %@", signOutError)
            }
            GIDSignIn.sharedInstance()?.signOut()
            performSegue(withIdentifier: "UnwindLoginSegue", sender: self)
        }
    }

    @IBAction func segmentChanged(_ sender: Any) {
        switch hostView.selectedSegmentIndex {
        case 0:
            UserDefaults.standard.set(0, forKey: "defaultView")
        case 1:
            UserDefaults.standard.set(1, forKey: "defaultView")
        default:
            break
        }
    }
}
