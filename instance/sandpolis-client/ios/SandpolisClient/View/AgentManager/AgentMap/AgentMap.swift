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
import CoreLocation
import SwiftEventBus

class AgentMap: UIViewController, MKMapViewDelegate {

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
			if let pin = AgentAnnotation(host) {
				map.addAnnotation(pin)
			}
		}

		SwiftEventBus.unregister(self)
		SwiftEventBus.onMainThread(self, name: "profileOnlineEvent") { result in
			if let profile = result?.object as? SandpolisProfile, self.findHost(profile) == nil, let annotation = AgentAnnotation(profile) {
				self.map.addAnnotation(annotation)
			}
		}
		SwiftEventBus.onMainThread(self, name: "profileOfflineEvent") { result in
			if let profile = result?.object as? SandpolisProfile, let annotation = self.findHost(profile) {
				self.map.removeAnnotation(annotation)
			}
		}

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

		// Remove navigation bar
		navigationController?.navigationBar.setBackgroundImage(UIImage(), for: .default)
		navigationController?.navigationBar.shadowImage = UIImage()
		navigationController?.navigationBar.isTranslucent = true
	}

	func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
		guard let annotation = annotation as? AgentAnnotation else { return nil }
		var annotationView = MKMarkerAnnotationView()
		if let dequedView = mapView.dequeueReusableAnnotationView(withIdentifier: "ClientAnnotation") as? MKMarkerAnnotationView {
			annotationView = dequedView
		} else {
			annotationView = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: "ClientAnnotation")
		}
		annotationView.markerTintColor = UIColor.red
		annotationView.glyphImage = annotation.profile.platformGlyph
		annotationView.clusteringIdentifier = "ClientAnnotation"

		return annotationView
	}

	func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {

		if let annotation = view.annotation as? MKClusterAnnotation {
			let alert = UIAlertController(title: "\(annotation.memberAnnotations.count) hosts selected", message: nil, preferredStyle: .actionSheet)
			alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
			alert.addAction(UIAlertAction(title: "Control Panel", style: .default) { _ in
				self.performSegue(withIdentifier: "ShowGroupControlPanelSegue", sender: annotation.memberAnnotations.map({($0 as! AgentAnnotation).profile}))
			})
			present(alert, animated: true)
		} else if let annotation = view.annotation as? AgentAnnotation {
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

	private func findHost(_ profile: SandpolisProfile) -> MKAnnotation? {
		return map.annotations.filter { annotation in
			if let annotation = annotation as? AgentAnnotation {
				return annotation.profile.cvid == profile.cvid
			} else {
				return false
			}
		}.first
	}
}
