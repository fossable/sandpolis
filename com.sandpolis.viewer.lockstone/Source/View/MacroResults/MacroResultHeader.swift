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

protocol CollapsibleTableViewHeaderDelegate {
    func toggleSection(_ header: MacroResultHeader, section: Int)
}

class MacroResultHeader: UITableViewHeaderFooterView {
    
    @IBOutlet weak var platform: UIImageView!
    @IBOutlet weak var hostname: UILabel!
    @IBOutlet weak var status: UILabel!
    @IBOutlet weak var progress: UIActivityIndicatorView!
    @IBOutlet weak var arrow: UILabel!

    var delegate: CollapsibleTableViewHeaderDelegate!
    var section: Int = 0
    
    func setContent(_ profile: SandpolisProfile) {
        hostname.text = profile.hostname
        switch profile.platform {
        case .linux:
            platform.image = UIImage(named: "linux")
        case .macos:
            platform.image = UIImage(named: "macos")
        case .windows:
            platform.image = UIImage(named: "windows")
        default:
            break
        }
    }
    
    //
    // Trigger toggle section when tapping on the header
    //
    @objc func didTapHeader(_ gestureRecognizer: UITapGestureRecognizer) {
        guard let cell = gestureRecognizer.view as? MacroResultHeader else {
            return
        }
        
        delegate?.toggleSection(self, section: cell.section)
    }
    
    func setCollapsed(_ collapsed: Bool) {
        //
        // Animate the arrow rotation (see Extensions.swf)
        //
        arrow.rotate(collapsed ? 0.0 : .pi / 2)
    }
    
    static var nib:UINib {
        return UINib(nibName: identifier, bundle: nil)
    }
    
    static var identifier: String {
        return "MacroResultHeader"
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
        
        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didTapHeader)))
    }
    
}

extension UIView {
    func rotate(_ toValue: CGFloat, duration: CFTimeInterval = 0.2) {
        let animation = CABasicAnimation(keyPath: "transform.rotation")
        
        animation.toValue = toValue
        animation.duration = duration
        animation.isRemovedOnCompletion = false
        animation.fillMode = CAMediaTimingFillMode.forwards
        
        self.layer.add(animation, forKey: nil)
    }
}
