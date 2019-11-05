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
import MapKit
import CoreLocation

class ClientMap: UIViewController, MKMapViewDelegate {

	@IBOutlet weak var map: MKMapView!
	
	var server: SandpolisServer!

	private var navShadow: UIImage?
	private var navTranslucent: Bool?
	private var navBackground: UIImage?

	override func viewDidLoad() {
		super.viewDidLoad()

		map.delegate = self
		if UserDefaults.standard.bool(forKey: "map.location") {
			map.showsUserLocation = true
			CLLocationManager().requestWhenInUseAuthorization()
		}

		for host in SandpolisUtil.connection.profiles {
			if let pin = ClientAnnotation(host) {
				map.addAnnotation(pin)
			}
		}
		SandpolisUtil.connection.registerHostUpdates(self.onHostUpdate)

		// Save navigation bar properties so they can be restored later
		navBackground = navigationController?.navigationBar.backgroundImage(for: .default)
		navShadow = navigationController?.navigationBar.shadowImage
		navTranslucent = navigationController?.navigationBar.isTranslucent
	}

	override func viewWillDisappear(_ animated: Bool) {
		super.viewWillDisappear(animated)

		// Restore navigation bar
		navigationController?.navigationBar.setBackgroundImage(navBackground, for: .default)
		navigationController?.navigationBar.shadowImage = navShadow
		navigationController?.navigationBar.isTranslucent = navTranslucent!
	}

	override func viewWillAppear(_ animated: Bool) {
		super.viewWillAppear(animated)

		map.isZoomEnabled = true
		map.isScrollEnabled = true

		// Remove navigation bar
		navigationController?.navigationBar.setBackgroundImage(UIImage(), for: .default)
		navigationController?.navigationBar.shadowImage = UIImage()
		navigationController?.navigationBar.isTranslucent = true
	}

	func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
		guard let annotation = annotation as? ClientAnnotation else { return nil }
		var annotationView = MKMarkerAnnotationView()
		let identifier = "ID"
		if let dequedView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? MKMarkerAnnotationView {
			annotationView = dequedView
		} else {
			annotationView = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: identifier)
		}
		annotationView.markerTintColor = UIColor.red
		annotationView.glyphImage = UIImage(named: "Image")

		annotationView.clusteringIdentifier = identifier

		return annotationView
	}

	func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
		guard let view = view as? MKMarkerAnnotationView else { return }
		view.glyphTintColor = .black

		let controller = UIAlertController(

			title: (view.annotation?.title)!,
			message: "Go to Control Panel?",
			preferredStyle: .actionSheet)

		let cancelAction = UIAlertAction(
			title: "Cancel",
			style: .cancel,
			handler: { (action) in view.glyphTintColor = .white }
		)

		let OKAction = UIAlertAction(
			title: "OK",
			style: .default,
			handler: { _ in self.performSegue(withIdentifier: "ToControl", sender: nil)
				view.glyphTintColor = .white
			}
		)
		controller.addAction(cancelAction)
		controller.addAction(OKAction)
		present(controller, animated: true, completion: nil)
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		let controlPanel = segue.destination as! ControlPanel

		let selected = map.selectedAnnotations
		if selected.count == 1, let pin = selected.first as? ClientAnnotation {
			controlPanel.profile = pin.profile
		} else {
			// TODO error
		}
	}

	func onHostUpdate(_ profile: SandpolisProfile) {
		DispatchQueue.main.async {
			let pin = self.map.annotations.filter { annotation in
				return (annotation as! ClientAnnotation).profile.cvid == profile.cvid
			}.first

			if profile.online {
				if pin == nil, let pin = ClientAnnotation(profile) {
					self.map.addAnnotation(pin)
				}
			} else {
				if let pin = pin {
					self.map.removeAnnotation(pin)
				}
			}
		}
	}
}
