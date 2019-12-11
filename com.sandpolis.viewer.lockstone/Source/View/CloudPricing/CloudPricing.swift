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
