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
		if let dequedView = mapView.dequeueReusableAnnotationView(withIdentifier: "ClientAnnotation") as? MKMarkerAnnotationView {
			annotationView = dequedView
		} else {
			annotationView = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: "ClientAnnotation")
		}
		annotationView.markerTintColor = UIColor.red
		annotationView.glyphImage = UIImage(named: "Image")

		annotationView.clusteringIdentifier = "ClientAnnotation"

		return annotationView
	}

	func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
		
		if let annotation = view.annotation as? MKClusterAnnotation {
			let alert = UIAlertController(title: "\(annotation.memberAnnotations.count) hosts selected", message: nil, preferredStyle: .actionSheet)
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
			alert.addAction(UIAlertAction(title: "Control Panel", style: .default) { _ in
				self.performSegue(withIdentifier: "ShowGroupControlPanelSegue", sender: annotation.memberAnnotations.map({($0 as! ClientAnnotation).profile}))
			})
			present(alert, animated: true)
		} else if let annotation = view.annotation as? ClientAnnotation {
			let alert = UIAlertController(title: annotation.title!, message: nil, preferredStyle: .actionSheet)
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
			alert.addAction(UIAlertAction(title: "Control Panel", style: .default) { _ in
				self.performSegue(withIdentifier: "ShowControlPanelSegue", sender: annotation.profile)
			})
			present(alert, animated: true)
		}
		
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "ShowControlPanelSegue",
			let dest = segue.destination as? ControlPanel {
			dest.profile = sender as? SandpolisProfile
		} else if segue.identifier == "ShowGroupControlPanelSegue",
			let dest = segue.destination as? GroupControlPanel {
			dest.profiles = sender as? [SandpolisProfile]
		}
	}

	func onHostUpdate(_ profile: SandpolisProfile) {
		DispatchQueue.main.async {
			let pin = self.map.annotations.filter { annotation in
				if let annotation = annotation as? ClientAnnotation {
					return annotation.profile.cvid == profile.cvid
				} else {
					return false
				}
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
