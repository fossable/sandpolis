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
import FirebaseDatabase

/// A runnable code snippet
class Macro {

	/// The user-friendly name of the macro
	var name: String

	/// Whether the macro is compatible with Windows
	var windows: Bool

	/// Whether the macro is compatible with macOS
	var macos: Bool

	/// Whether the macro is compatible with Linux
	var linux: Bool

	/// The macro's content
	var script: String

	init(name: String, script: String, windows: Bool, macos: Bool, linux: Bool) {
		self.name = name
		self.windows = windows
		self.macos = macos
		self.linux = linux
		self.script = script
	}

	convenience init?(_ snapshot: DataSnapshot) {
		guard let content = snapshot.value as? [String: AnyObject],
			let name = content["name"] as? String,
			let windows = content["windows"] as? Bool,
			let macos = content["macos"] as? Bool,
			let linux = content["linux"] as? Bool,
			let script = content["script"] as? String
			else {
				return nil
		}

		self.init(name: name, script: script, windows: windows, macos: macos, linux: linux)
	}
}
