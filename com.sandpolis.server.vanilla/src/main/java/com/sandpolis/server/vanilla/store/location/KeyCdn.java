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
package com.sandpolis.server.vanilla.store.location;

import java.util.Set;

import static com.sandpolis.core.instance.DocumentBindings.Profile.Instance.Client.IpLocation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.instance.DocumentBindings.Profile.Instance.Client.IpLocation;

public class KeyCdn extends AbstractGeolocationService {

	private static final ImmutableBiMap<Integer, String> attrMap = new ImmutableBiMap.Builder<Integer, String>()
			.put(AS_CODE, "asn")//
			.put(CITY, "city")//
			.put(CONTINENT, "continent_name")//
			.put(CONTINENT_CODE, "continent_code")//
			.put(COUNTRY, "country_name")//
			.put(COUNTRY_CODE, "country_code")//
			.put(ISP, "isp")//
			.put(LATITUDE, "latitude")//
			.put(LONGITUDE, "lonitude")//
			.put(METRO_CODE, "metro_code")//
			.put(POSTAL_CODE, "postal_code")//
			.put(REGION, "region_name")//
			.put(REGION_CODE, "region_code")//
			.put(TIMEZONE, "timezone")//
			.build();

	public KeyCdn() {
		super(attrMap, "https");
	}

	@Override
	protected String buildQuery(String ip, Set<Integer> fields) {
		return String.format("%s://tools.keycdn.com/geo.json?host=%s", protocol, ip);
	}

	@Override
	protected IpLocation parseLocation(String result) throws Exception {
		IpLocation location = new IpLocation(null);
		new ObjectMapper().readTree(result).path("data").path("geo").forEach(node -> {
			node.fields().forEachRemaining(entry -> {
				switch (attrMap.inverse().get(entry.getKey())) {
				case AS_CODE:
					location.asCode().set(entry.getValue().asInt());
					break;
				case CITY:
					location.city().set(entry.getValue().asText());
					break;
				case CONTINENT:
					location.continent().set(entry.getValue().asText());
					break;
				case CONTINENT_CODE:
					location.continentCode().set(entry.getValue().asText());
					break;
				case COUNTRY:
					location.country().set(entry.getValue().asText());
					break;
				case COUNTRY_CODE:
					location.countryCode().set(entry.getValue().asText());
					break;
				case ISP:
					location.isp().set(entry.getValue().asText());
					break;
				case LATITUDE:
					location.latitude().set(entry.getValue().asDouble());
					break;
				case LONGITUDE:
					location.longitude().set(entry.getValue().asDouble());
					break;
				case METRO_CODE:
					location.metroCode().set(entry.getValue().asInt());
					break;
				case POSTAL_CODE:
					location.postalCode().set(entry.getValue().asText());
					break;
				case REGION:
					location.region().set(entry.getValue().asText());
					break;
				case REGION_CODE:
					location.regionCode().set(entry.getValue().asText());
					break;
				case TIMEZONE:
					location.timezone().set(entry.getValue().asText());
					break;
				}
			});
		});

		return location;
	}
}
