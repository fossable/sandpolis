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

import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.AS_CODE;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.CITY;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.CONTINENT;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.CONTINENT_CODE;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.COUNTRY;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.COUNTRY_CODE;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.CURRENCY;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.DISTRICT;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.ISP;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.LATITUDE;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.LONGITUDE;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.ORGANIZATION;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.POSTAL_CODE;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.REGION;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.REGION_CODE;
import static com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation.TIMEZONE;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.instance.StateTree.VirtProfile.VirtClient.VirtIpLocation;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.server.location.AbstractGeolocationService;

/**
 * This {@link AbstractGeolocationService} implementation interacts with
 * <a href="https://ip-api.com/">https://ip-api.com</a>.
 */
public final class IpApi extends AbstractGeolocationService {

	/**
	 * The fields provided by the location service associated with {@link Oid}s.
	 */
	private static final ImmutableBiMap<Oid<?>, String> JSON_FIELDS = new ImmutableBiMap.Builder<Oid<?>, String>()
			.put(AS_CODE, "as") //
			.put(CITY, "city") //
			.put(CONTINENT, "continent") //
			.put(CONTINENT_CODE, "continentCode") //
			.put(COUNTRY, "country") //
			.put(COUNTRY_CODE, "countryCode") //
			.put(CURRENCY, "currency") //
			.put(DISTRICT, "district") //
			.put(ISP, "isp") //
			.put(LATITUDE, "lat") //
			.put(LONGITUDE, "lon") //
			.put(ORGANIZATION, "org") //
			.put(POSTAL_CODE, "zip") //
			.put(REGION, "regionName") //
			.put(REGION_CODE, "region") //
			.put(TIMEZONE, "timezone") //
			.build();

	private String key;

	public IpApi() {
		super("http");
	}

	public IpApi(String key) {
		super("https");
		this.key = Objects.requireNonNull(key);
	}

	@Override
	protected String buildQuery(String ip, Oid<?>... fields) {
		if (fields.length == 0) {
			return String.format("%s://ip-api.com/json/%s", protocol, ip);
		} else {
			return String.format("%s://ip-api.com/json/%s?fields=%s", protocol, ip, Arrays.stream(fields)
					.filter(JSON_FIELDS::containsKey).map(JSON_FIELDS::get).collect(Collectors.joining(",")));
		}
	}

	@Override
	protected VirtIpLocation parseLocation(String result) throws Exception {
		VirtIpLocation location = new VirtIpLocation(null);
		new ObjectMapper().readTree(result).fields().forEachRemaining(entry -> {
			switch (entry.getKey()) {
			case "as":
				location.asCode().set(entry.getValue().asInt());
				break;
			case "city":
				location.city().set(entry.getValue().asText());
				break;
			case "continent":
				location.continent().set(entry.getValue().asText());
				break;
			case "continentCode":
				location.continentCode().set(entry.getValue().asText());
				break;
			case "country":
				location.country().set(entry.getValue().asText());
				break;
			case "countryCode":
				location.countryCode().set(entry.getValue().asText());
				break;
			case "currency":
				location.currency().set(entry.getValue().asText());
				break;
			case "district":
				location.district().set(entry.getValue().asText());
				break;
			case "isp":
				location.isp().set(entry.getValue().asText());
				break;
			case "lat":
				location.latitude().set(entry.getValue().asDouble());
				break;
			case "long":
				location.longitude().set(entry.getValue().asDouble());
				break;
			case "org":
				location.organization().set(entry.getValue().asText());
				break;
			case "zip":
				location.postalCode().set(entry.getValue().asText());
				break;
			case "regionName":
				location.region().set(entry.getValue().asText());
				break;
			case "region":
				location.regionCode().set(entry.getValue().asText());
				break;
			case "timezone":
				location.timezone().set(entry.getValue().asText());
				break;
			}
		});

		return location;
	}
}
