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
import Firebase
import SwiftKeychainWrapper

class Login: UIViewController {

	@IBOutlet weak var loginAccount: UIView!
	@IBOutlet weak var loginServer: UIView!
	@IBOutlet weak var createAccount: UIView!
	@IBOutlet weak var progress: UIActivityIndicatorView!

	override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
		get { return UIInterfaceOrientationMask.portrait }
	}

	override var shouldAutorotate: Bool {
		get { return false }
	}

	override var preferredInterfaceOrientationForPresentation: UIInterfaceOrientation {
		get { return UIInterfaceOrientation.portrait }
	}

	override var preferredStatusBarStyle: UIStatusBarStyle {
		return .lightContent
	}

	override func viewDidLoad() {
		super.viewDidLoad()

		if UserDefaults.standard.bool(forKey: "login.auto") {
			progress.startAnimating()
			switch UserDefaults.standard.string(forKey: "login.type") {
			case "cloud":
				// Set auth listener
				AppDelegate.requireFirebase()
				let loginListener = Auth.auth().addStateDidChangeListener() { auth, user in
					if user != nil {
						self.progress.stopAnimating()
						self.performSegue(withIdentifier: "LoginCompleteSegue", sender: nil)
					}
				}

				// Set auth timer in case the automatic login fails
				DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
					Auth.auth().removeStateDidChangeListener(loginListener)
					if Auth.auth().currentUser == nil {
						self.progress.stopAnimating()
						self.openLoginAccount()
					}
				}
				break
			case "direct":
				if let address = KeychainWrapper.standard.string(forKey: "login.direct.address"),
					let username = KeychainWrapper.standard.string(forKey: "login.direct.username"),
					let password = KeychainWrapper.standard.string(forKey: "login.direct.password") {

					login(address: address, username: username, password: password) { connection in
						DispatchQueue.main.async {
							self.progress.stopAnimating()
							if let connection = connection {
								SandpolisUtil.connection = connection
								self.performSegue(withIdentifier: "DirectLoginCompleteSegue", sender: address)
							} else {
								self.openLoginServer()
							}
						}
					}
				} else {
					progress.stopAnimating()
					openLoginServer()
				}
				break
			default:
				fatalError()
			}
		} else {
			switch UserDefaults.standard.string(forKey: "login.type") {
			case "cloud":
				openLoginAccount()
			case "direct":
				openLoginServer()
			default:
				openCreateAccount()
			}
		}
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "LoginEmbed",
			let login = segue.destination as? LoginAccount {
			login.loginContainer = self
		} else if segue.identifier == "ForgotPasswordEmbed",
			let forgotPassword = segue.destination as? LoginServer {
			forgotPassword.loginContainer = self
		} else if segue.identifier == "CreateAccountEmbed",
			let createAccount = segue.destination as? CreateAccount {
			createAccount.loginContainer = self
		} else if segue.identifier == "DirectLoginCompleteSegue",
			let dest = segue.destination as? ClientManager {
			dest.serverName = sender as? String
		}
	}

	func openLoginAccount() {
		UIView.animate(withDuration: 0.5, animations: {
			self.loginServer.alpha = 0.0
			self.createAccount.alpha = 0.0
		}, completion: { _ in
			UIView.animate(withDuration: 0.5) {
				self.loginAccount.alpha = 1.0
			}
		})
	}

	func openLoginServer() {
		UIView.animate(withDuration: 0.5, animations: {
			self.loginAccount.alpha = 0.0
			self.createAccount.alpha = 0.0
		}, completion: { _ in
			UIView.animate(withDuration: 0.5) {
				self.loginServer.alpha = 1.0
			}
		})
	}

	func openCreateAccount() {
		UIView.animate(withDuration: 0.5, animations: {
			self.loginAccount.alpha = 0.0
			self.loginServer.alpha = 0.0
		}, completion: { _ in
			UIView.animate(withDuration: 0.5) {
				self.createAccount.alpha = 1.0
			}
		})
	}

	// Called on logout
	@IBAction func prepareForUnwind(segue: UIStoryboardSegue) {
		openLoginAccount()
	}

}
