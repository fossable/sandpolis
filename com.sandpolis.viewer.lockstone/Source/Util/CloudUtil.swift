//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
import Foundation
import FirebaseAuth

class CloudUtil {

	static func createCloudInstance(hostname: String, username: String, password: String, location: String, _ completion: @escaping(NSDictionary?, Error?) -> Void) {
		Auth.auth().currentUser?.getIDToken { token, error in
			guard error == nil else {
				completion(nil, error)
				return
			}

			var request = URLRequest(url: URL(string: "https://api.sandpolis.com/v1/cloud/instance/create")!)
			request.addValue("Bearer " + token!, forHTTPHeaderField: "Authorization")
			request.httpMethod = "POST"
			request.httpBody = try? JSONSerialization.data(withJSONObject: [
				"location": location,
				"hostname": hostname,
				"username": username,
				"password": password
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

	static func addCloudClient(token: String, _ completion: @escaping(NSDictionary?, Error?) -> Void) {
		Auth.auth().currentUser?.getIDToken { token, error in
			guard error == nil else {
				completion(nil, error)
				return
			}

			var request = URLRequest(url: URL(string: "https://api.sandpolis.com/v1/cloud/client/add")!)
			request.addValue("Bearer " + token!, forHTTPHeaderField: "Authorization")
			request.httpMethod = "POST"
			request.httpBody = try? JSONSerialization.data(withJSONObject: [
				"token": token,
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
