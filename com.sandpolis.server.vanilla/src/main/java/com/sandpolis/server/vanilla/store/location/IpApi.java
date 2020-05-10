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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sandpolis.core.instance.DocumentBindings.Profile.Instance.Client.IpLocation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.instance.DocumentBindings.Profile.Instance.Client.IpLocation;

public class IpApi extends AbstractGeolocationService {

	private static final ImmutableBiMap<Integer, String> attrMap = new ImmutableBiMap.Builder<Integer, String>()
			.put(AS_CODE, "as")//
			.put(CITY, "city")//
			.put(CONTINENT, "continent")//
			.put(CONTINENT_CODE, "continentCode")//
			.put(COUNTRY, "country")//
			.put(COUNTRY_CODE, "countryCode")//
			.put(CURRENCY, "currency")//
			.put(DISTRICT, "district")//
			.put(ISP, "isp")//
			.put(LATITUDE, "lat")//
			.put(LONGITUDE, "lon")//
			.put(ORGANIZATION, "org")//
			.put(POSTAL_CODE, "zip")//
			.put(REGION, "regionName")//
			.put(REGION_CODE, "region")//
			.put(TIMEZONE, "timezone")//
			.build();

	private String key;

	public IpApi() {
		super(attrMap, "http");
	}

	public IpApi(String key) {
		super(attrMap, "https");
		this.key = Objects.requireNonNull(key);
	}

	@Override
	protected String buildQuery(String ip, Set<Integer> fields) {
		return String.format("%s://ip-api.com/json/%s?fields=%s", protocol, ip,
				fields.stream().map(attrMap::get).collect(Collectors.joining(",")));
	}

	@Override
	protected IpLocation parseLocation(String result) throws Exception {
		IpLocation location = new IpLocation(null);
		new ObjectMapper().readTree(result).fields().forEachRemaining(entry -> {
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
			case CURRENCY:
				location.currency().set(entry.getValue().asText());
				break;
			case DISTRICT:
				location.district().set(entry.getValue().asText());
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
			case ORGANIZATION:
				location.organization().set(entry.getValue().asText());
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

		return location;
	}
}
