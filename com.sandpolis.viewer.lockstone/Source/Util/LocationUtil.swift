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
import os

class LocationUtil {

	/// Query the assigned location of an IP address from ip-api.com.
	///
	/// - Parameters:
	///   - ip: The IP address or DNS name
	///   - fields: The information fields to request
	///   - completion: The completion handler
	static func queryIpLocation(_ ip: String, fields: [String] = ["country", "countryCode", "regionName", "city", "lat", "lon"], _ completion: @escaping(NSDictionary?, Error?) -> Void) {
		os_log("Querying location information for host: %s", ip)

		var request = URLRequest(url: URL(string: "http://ip-api.com/json/\(ip)?fields=status,\(fields.joined(separator: ","))")!)
		request.httpMethod = "GET"

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
