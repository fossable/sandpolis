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
package com.sandpolis.core.server.location.services;

import static com.sandpolis.core.instance.state.VirtIpLocation.AS_CODE;
import static com.sandpolis.core.instance.state.VirtIpLocation.CITY;
import static com.sandpolis.core.instance.state.VirtIpLocation.CONTINENT;
import static com.sandpolis.core.instance.state.VirtIpLocation.CONTINENT_CODE;
import static com.sandpolis.core.instance.state.VirtIpLocation.COUNTRY;
import static com.sandpolis.core.instance.state.VirtIpLocation.COUNTRY_CODE;
import static com.sandpolis.core.instance.state.VirtIpLocation.ISP;
import static com.sandpolis.core.instance.state.VirtIpLocation.LATITUDE;
import static com.sandpolis.core.instance.state.VirtIpLocation.LONGITUDE;
import static com.sandpolis.core.instance.state.VirtIpLocation.METRO_CODE;
import static com.sandpolis.core.instance.state.VirtIpLocation.POSTAL_CODE;
import static com.sandpolis.core.instance.state.VirtIpLocation.REGION;
import static com.sandpolis.core.instance.state.VirtIpLocation.REGION_CODE;
import static com.sandpolis.core.instance.state.VirtIpLocation.TIMEZONE;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.instance.state.VirtIpLocation;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.server.location.AbstractGeolocationService;

/**
 * This {@link AbstractGeolocationService} implementation interacts with
 * <a href="https://tools.keycdn.com/geo">https://tools.keycdn.com/geo</a>.
 */
public final class KeyCdn extends AbstractGeolocationService {

	/**
	 * The fields provided by the location service associated with {@link Oid}s.
	 */
	private static final ImmutableBiMap<Oid, String> JSON_FIELDS = new ImmutableBiMap.Builder<Oid, String>()
			.put(AS_CODE, "asn") //
			.put(CITY, "city") //
			.put(CONTINENT, "continent_name") //
			.put(CONTINENT_CODE, "continent_code") //
			.put(COUNTRY, "country_name") //
			.put(COUNTRY_CODE, "country_code") //
			.put(ISP, "isp") //
			.put(LATITUDE, "latitude") //
			.put(LONGITUDE, "lonitude") //
			.put(METRO_CODE, "metro_code") //
			.put(POSTAL_CODE, "postal_code") //
			.put(REGION, "region_name") //
			.put(REGION_CODE, "region_code") //
			.put(TIMEZONE, "timezone") //
			.build();

	public KeyCdn() {
		super("https");
	}

	@Override
	protected String buildQuery(String ip, Oid... fields) {
		// TODO request fields
		Arrays.stream(fields).filter(JSON_FIELDS::containsKey);
		return String.format("%s://tools.keycdn.com/geo.json?host=%s", protocol, ip);
	}

	@Override
	protected VirtIpLocation parseLocation(String result) throws Exception {
		VirtIpLocation location = new VirtIpLocation(null);
		new ObjectMapper().readTree(result).path("data").path("geo").forEach(node -> {
			node.fields().forEachRemaining(entry -> {
				switch (entry.getKey()) {
				case "asn":
					location.asCode().set(entry.getValue().asInt());
					break;
				case "city":
					location.city().set(entry.getValue().asText());
					break;
				case "continent_name":
					location.continent().set(entry.getValue().asText());
					break;
				case "continent_code":
					location.continentCode().set(entry.getValue().asText());
					break;
				case "country_name":
					location.country().set(entry.getValue().asText());
					break;
				case "country_code":
					location.countryCode().set(entry.getValue().asText());
					break;
				case "isp":
					location.isp().set(entry.getValue().asText());
					break;
				case "latitude":
					location.latitude().set(entry.getValue().asDouble());
					break;
				case "longitude":
					location.longitude().set(entry.getValue().asDouble());
					break;
				case "metro_code":
					location.metroCode().set(entry.getValue().asInt());
					break;
				case "postal_code":
					location.postalCode().set(entry.getValue().asText());
					break;
				case "region_name":
					location.region().set(entry.getValue().asText());
					break;
				case "region_code":
					location.regionCode().set(entry.getValue().asText());
					break;
				case "timezone":
					location.timezone().set(entry.getValue().asText());
					break;
				}
			});
		});

		return location;
	}
}
