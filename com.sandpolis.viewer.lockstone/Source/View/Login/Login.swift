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

class Login: UIViewController {

	@IBOutlet weak var loginAccount: UIView!
	@IBOutlet weak var loginServer: UIView!
	@IBOutlet weak var createAccount: UIView!

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

	private var loginListener: AuthStateDidChangeListenerHandle!

	override func viewDidLoad() {
		super.viewDidLoad()
		openLoginAccount()
	}

	override func viewWillAppear(_ animated: Bool) {
		super.viewWillAppear(animated)

		loginListener = Auth.auth().addStateDidChangeListener() { auth, user in
			if user != nil {
				self.performSegue(withIdentifier: "LoginCompleteSegue", sender: nil)
			}
		}
	}

	override func viewWillDisappear(_ animated: Bool) {
		super.viewWillDisappear(animated)

		Auth.auth().removeStateDidChangeListener(loginListener!)
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
