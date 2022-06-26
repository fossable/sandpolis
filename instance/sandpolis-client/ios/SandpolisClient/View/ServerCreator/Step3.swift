//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import UIKit
import MapKit

class Step3: UIViewController {

	private let locations = [
		["id": "us-east-2", "name": "Ohio, United States", "coordinate": CLLocationCoordinate2D(latitude: 40.4173, longitude: -82.9071)],
		["id": "us-east-1", "name": "Virginia, United States", "coordinate": CLLocationCoordinate2D(latitude: 37.4316, longitude: -78.6569)],
		["id": "us-west-1", "name": "California, United States", "coordinate": CLLocationCoordinate2D(latitude: 36.7783, longitude: -119.4179)],
		["id": "us-west-2", "name": "Oregon, United States", "coordinate": CLLocationCoordinate2D(latitude: 43.8041, longitude: -120.5542)],
		["id": "ap-east-1", "name": "Hong Kong", "coordinate": CLLocationCoordinate2D(latitude: 22.3193, longitude: 114.1694)],
		["id": "ap-south-1", "name": "Mumbai, India", "coordinate": CLLocationCoordinate2D(latitude: 19.0760, longitude: 72.8777)],
		["id": "ap-northeast-2", "name": "Seoul, South Korea", "coordinate": CLLocationCoordinate2D(latitude: 37.5665, longitude: 126.9780)],
		["id": "ap-northeast-1", "name": "Tokyo, Japan", "coordinate": CLLocationCoordinate2D(latitude: 35.6762, longitude: 139.6503)],
		["id": "ca-central-1", "name": "Canada", "coordinate": CLLocationCoordinate2D(latitude: 56.1304, longitude: -106.3468)],
		["id": "cn-north-1", "name": "Beijing, China", "coordinate": CLLocationCoordinate2D(latitude: 39.9042, longitude: 116.4074)],
		["id": "eu-central-1", "name": "Frankfurt, Germany", "coordinate": CLLocationCoordinate2D(latitude: 50.1109, longitude: 8.6821)],
		["id": "eu-west-1", "name": "Ireland", "coordinate": CLLocationCoordinate2D(latitude: 53.1424, longitude: -7.6921)],
		["id": "eu-west-2", "name": "London, England", "coordinate": CLLocationCoordinate2D(latitude: 51.5074, longitude: -0.1278)],
		["id": "eu-west-3", "name": "Paris, France", "coordinate": CLLocationCoordinate2D(latitude: 48.8566, longitude: 2.3522)],
		["id": "sa-east-1", "name": "Sao Paulo, Brazil", "coordinate": CLLocationCoordinate2D(latitude: -23.5505, longitude: -46.6333)]
	]

	@IBOutlet weak var map: MKMapView!
	@IBOutlet weak var locationButton: UIButton!

	var parentController: ServerCreator!

	override func viewDidLoad() {
		super.viewDidLoad()
	}

	@IBAction func changeLocation(_ sender: Any) {
		let alert = UIAlertController(title: "Available Locations", message: nil, preferredStyle: .actionSheet)
		alert.popoverPresentationController?.sourceView = locationButton

		for data in locations {
			alert.addAction(UIAlertAction(title: data["name"] as? String, style: .default) { action in
				self.locationButton.setTitle(data["id"] as? String, for: .normal)
				self.map.setCenter(data["coordinate"] as! CLLocationCoordinate2D, animated: true)
				self.map.removeAnnotations(self.map.annotations)

				let pin = MKPointAnnotation()
				pin.coordinate = data["coordinate"] as! CLLocationCoordinate2D
				self.map.addAnnotation(pin)
			})
		}

		present(alert, animated: true, completion: nil)
	}

	@IBAction func back(_ sender: Any) {
		parentController.showStep2()
	}

	@IBAction func next(_ sender: Any) {
		parentController.showStep4()
	}
}
