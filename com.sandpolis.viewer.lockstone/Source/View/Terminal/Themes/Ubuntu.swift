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
import Foundation
import UIKit

// This source file is part of the https://github.com/ColdGrub1384/Pisth open source project
//
// Copyright (c) 2017 - 2018 Adrian Labbé
// Licensed under Apache License v2.0
//
// See https://raw.githubusercontent.com/ColdGrub1384/Pisth/master/LICENSE for license information

open class UbuntuTheme: TerminalTheme {

	open override var backgroundColor: UIColor? {
		return UIColor(red: 48/255, green: 10/255, blue: 36/255, alpha: 1)
	}

	open override var foregroundColor: UIColor? {
		return UIColor(red: 255/255, green: 253/255, blue: 244/255, alpha: 1)
	}

	open override var cursorColor: UIColor? {
		return UIColor(red: 251/255, green: 251/255, blue: 251/255, alpha: 1)
	}
}
