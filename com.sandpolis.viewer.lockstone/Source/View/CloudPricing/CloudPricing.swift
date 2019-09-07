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
import StoreKit

class CloudPricing: UIViewController {

    @IBOutlet weak var monthlyButton: UIButton!
    @IBOutlet weak var yearlyButton: UIButton!

	private var monthlyProduct: SKProduct?
	private var yearlyProduct: SKProduct?

    override var preferredStatusBarStyle: UIStatusBarStyle {
		return .lightContent
	}

	override func viewDidLoad() {
		SKPaymentQueue.default().add(self)
		// Get the product list
		let request = SKProductsRequest(productIdentifiers: ["cloud_monthly", "cloud_yearly"])
		request.delegate = self
		request.start()
	}

    @IBAction func buyMonthly(_ sender: Any) {
		if let product = monthlyProduct {
			SKPaymentQueue.default().add(SKPayment(product: product))
		}
    }

    @IBAction func buyYearly(_ sender: Any) {
    }

    @available(iOS 10.0, *)
    @IBAction func openWebsite(_ sender: Any) {
		UIApplication.shared.open(URL(string: "https://sandpolis.com")!, options: [:], completionHandler: nil)
    }
}

extension CloudPricing: SKProductsRequestDelegate {
	func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse){
		for product in response.products {
			switch product.productIdentifier {
			case "cloud_monthly":
				monthlyProduct = product
				monthlyButton.isEnabled = true
				monthlyButton.setTitle("One cloud server for \(product.priceLocale.currencySymbol!)\(product.price) / month", for: .normal)
			case "cloud_yearly":
				yearlyProduct = product
				yearlyButton.isEnabled = true
				yearlyButton.setTitle("One cloud server for \(product.priceLocale.currencySymbol!)\(product.price) / year", for: .normal)
			default:
				break
			}
		}
	}
}

extension CloudPricing: SKPaymentTransactionObserver {
	func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
		for transaction in transactions {
			switch transaction.transactionState {
			case .purchased:
				print("Purchased")
                SKPaymentQueue.default().finishTransaction(transaction)
				//self.performSegue(withIdentifier: "CreateServerSegue", sender: self)
			case .purchasing:
				print("Purchasing")
			case .failed:
				print("Failed:", transaction.error)
			case .restored:
				print("Restored")
			case .deferred:
				print("Deferred")
			default:
				break
			}
		}
	}
}
