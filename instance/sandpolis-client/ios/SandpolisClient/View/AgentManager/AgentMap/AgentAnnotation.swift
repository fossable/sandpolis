//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
import MapKit

class AgentAnnotation: NSObject, MKAnnotation {

	var coordinate: CLLocationCoordinate2D
	var title: String?

	let profile: SandpolisProfile

	init?(_ profile: SandpolisProfile) {

		self.profile = profile
		self.title = profile.hostname.value as? String
		if let latitude = profile.ipLocationLatitude.value as? Float32, let longitude = profile.ipLocationLongitude.value as? Float32 {
			self.coordinate = CLLocationCoordinate2DMake(CLLocationDegrees(latitude), CLLocationDegrees(longitude))
		} else {
			return nil
		}
	}
}
