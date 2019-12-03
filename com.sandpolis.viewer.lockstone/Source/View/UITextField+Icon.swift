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

extension UITextField {
	func setLeftIcon(_ image: String) {
		let img = UIImageView(frame: CGRect(x: 10, y: 5, width: 23, height: 23))
		img.image = UIImage(named: image)
		let container = UIView(frame: CGRect(x: 20, y: 0, width: 34, height: 34))
		container.addSubview(img)
		self.leftView = container

		// Just to be sure
		self.leftViewMode = .always
	}
}
