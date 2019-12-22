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
								connection.openProfileStream()

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
			let dest = segue.destination as? LoginAccount {
			dest.loginContainer = self
		} else if segue.identifier == "ForgotPasswordEmbed",
			let dest = segue.destination as? LoginServer {
			dest.loginContainer = self
		} else if segue.identifier == "CreateAccountEmbed",
			let dest = segue.destination as? CreateAccount {
			dest.loginContainer = self
		} else if segue.identifier == "DirectLoginCompleteSegue",
			let dest = segue.destination as? ClientManagerWrapper {
			dest.name = sender as? String
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
