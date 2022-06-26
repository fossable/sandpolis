//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.location.services;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.AgentOid.IpLocationOid;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.server.location.AbstractGeolocationService;
import org.s7s.core.server.location.IpLocation;

/**
 * This {@link AbstractGeolocationService} implementation interacts with
 * <a href="https://ip-api.com/">https://ip-api.com</a>.
 */
public final class IpApi extends AbstractGeolocationService {

	/**
	 * The fields provided by the location service associated with {@link Oid}s.
	 */
	private static final ImmutableBiMap<Oid, String> JSON_FIELDS = new ImmutableBiMap.Builder<Oid, String>()
			.put(IpLocationOid.AS_CODE, "as") //
			.put(IpLocationOid.CITY, "city") //
			.put(IpLocationOid.CONTINENT, "continent") //
			.put(IpLocationOid.CONTINENT_CODE, "continentCode") //
			.put(IpLocationOid.COUNTRY, "country") //
			.put(IpLocationOid.COUNTRY_CODE, "countryCode") //
			.put(IpLocationOid.CURRENCY, "currency") //
			.put(IpLocationOid.DISTRICT, "district") //
			.put(IpLocationOid.ISP, "isp") //
			.put(IpLocationOid.LATITUDE, "lat") //
			.put(IpLocationOid.LONGITUDE, "lon") //
			.put(IpLocationOid.ORGANIZATION, "org") //
			.put(IpLocationOid.POSTAL_CODE, "zip") //
			.put(IpLocationOid.REGION, "regionName") //
			.put(IpLocationOid.REGION_CODE, "region") //
			.put(IpLocationOid.TIMEZONE, "timezone") //
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
	protected String buildQuery(String ip, Oid... fields) {
		if (fields.length == 0) {
			return String.format("%s://ip-api.com/json/%s", protocol, ip);
		} else {
			return String.format("%s://ip-api.com/json/%s?fields=%s", protocol, ip, Arrays.stream(fields)
					.filter(JSON_FIELDS::containsKey).map(JSON_FIELDS::get).collect(Collectors.joining(",")));
		}
	}

	@Override
	protected IpLocation parseLocation(String result) throws Exception {
		IpLocation location = new IpLocation(null);
		new ObjectMapper().readTree(result).fields().forEachRemaining(entry -> {
			switch (entry.getKey()) {
			case "as":
				location.set(IpLocationOid.AS_CODE, entry.getValue().asInt());
				break;
			case "city":
				location.set(IpLocationOid.CITY, entry.getValue().asText());
				break;
			case "continent":
				location.set(IpLocationOid.CONTINENT, entry.getValue().asText());
				break;
			case "continentCode":
				location.set(IpLocationOid.CONTINENT_CODE, entry.getValue().asText());
				break;
			case "country":
				location.set(IpLocationOid.COUNTRY, entry.getValue().asText());
				break;
			case "countryCode":
				location.set(IpLocationOid.COUNTRY_CODE, entry.getValue().asText());
				break;
			case "currency":
				location.set(IpLocationOid.CURRENCY, entry.getValue().asText());
				break;
			case "district":
				location.set(IpLocationOid.DISTRICT, entry.getValue().asText());
				break;
			case "isp":
				location.set(IpLocationOid.ISP, entry.getValue().asText());
				break;
			case "lat":
				location.set(IpLocationOid.LATITUDE, entry.getValue().asDouble());
				break;
			case "long":
				location.set(IpLocationOid.LONGITUDE, entry.getValue().asDouble());
				break;
			case "org":
				location.set(IpLocationOid.ORGANIZATION, entry.getValue().asText());
				break;
			case "zip":
				location.set(IpLocationOid.POSTAL_CODE, entry.getValue().asText());
				break;
			case "regionName":
				location.set(IpLocationOid.REGION, entry.getValue().asText());
				break;
			case "region":
				location.set(IpLocationOid.REGION_CODE, entry.getValue().asText());
				break;
			case "timezone":
				location.set(IpLocationOid.TIMEZONE, entry.getValue().asText());
				break;
			}
		});

		return location;
	}
}
