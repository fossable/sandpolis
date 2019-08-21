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

/// Represents a Sandpolis server
class SandpolisServer {

    /// The server name
    var name: String
    
    /// The server address
    var address: String
    
    /// The viewer username for the server
    var username: String
    
    /// The viewer password for the server (unsalted SHA256)
    var password: String
	
	/// Whether the server is a cloud server
	var cloud: Bool
    
    /// Whether the server is online
    var online: Bool?
    
    /// The server's country code
    var countryCode: String?
    
	init(name: String, address: String, username: String, password: String, cloud: Bool) {
        self.name = name
        self.address = address
        self.username = username
        self.password = password
		self.cloud = cloud
    }
	
	convenience init?(_ snapshot: DataSnapshot) {
		guard let content = snapshot.value as? [String: AnyObject],
			let name = content["name"] as? String,
			let address = content["address"] as? String,
			let username = content["username"] as? String,
			let password = content["password"] as? String,
			let cloud = content["cloud"] as? Bool
			else {
				return nil
		}
		
		self.init(name: name, address: address, username: username, password: password, cloud: cloud)
	}
}
