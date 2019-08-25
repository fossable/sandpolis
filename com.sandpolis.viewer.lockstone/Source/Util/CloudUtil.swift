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
import Foundation
import FirebaseAuth

class CloudUtil {

	static func createCloudInstance(subscription: String, hostname: String, _ completion: @escaping(NSDictionary?, Error?) -> Void) {
		Auth.auth().currentUser?.getIDToken { token, error in
			guard error == nil else {
				completion(nil, error)
				return
			}
			
			var request = URLRequest(url: URL(string: "https://api.sandpolis.com/v1/cloud/instance/create")!)
			request.addValue("Bearer " + token!, forHTTPHeaderField: "Authorization")
			request.httpMethod = "POST"
			request.httpBody = try? JSONSerialization.data(withJSONObject: [
				"subscription": subscription,
				"hostname": hostname
			], options: [])
			
			URLSession.shared.dataTask(with: request) { data, response, error in
				if let content = data {
					do {
						if let json = try JSONSerialization.jsonObject(with: content, options: .allowFragments) as? NSDictionary {
							completion(json, error)
						}
					} catch {
						completion(nil, error)
					}
				} else {
					completion(nil, error)
				}
			}.resume()
		}
	}
}
