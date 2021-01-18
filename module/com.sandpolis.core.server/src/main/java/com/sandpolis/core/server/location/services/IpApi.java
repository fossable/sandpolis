//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.location.services;

import static com.sandpolis.core.instance.state.InstanceOid.InstanceOid;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.instance.state.VirtIpLocation;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.server.location.AbstractGeolocationService;

/**
 * This {@link AbstractGeolocationService} implementation interacts with
 * <a href="https://ip-api.com/">https://ip-api.com</a>.
 */
public final class IpApi extends AbstractGeolocationService {

	/**
	 * The fields provided by the location service associated with {@link Oid}s.
	 */
	private static final ImmutableBiMap<Oid, String> JSON_FIELDS = new ImmutableBiMap.Builder<Oid, String>()
			.put(InstanceOid().profile.agent.iplocation.as_code, "as") //
			.put(InstanceOid().profile.agent.iplocation.city, "city") //
			.put(InstanceOid().profile.agent.iplocation.continent, "continent") //
			.put(InstanceOid().profile.agent.iplocation.continent_code, "continentCode") //
			.put(InstanceOid().profile.agent.iplocation.country, "country") //
			.put(InstanceOid().profile.agent.iplocation.country_code, "countryCode") //
			.put(InstanceOid().profile.agent.iplocation.currency, "currency") //
			.put(InstanceOid().profile.agent.iplocation.district, "district") //
			.put(InstanceOid().profile.agent.iplocation.isp, "isp") //
			.put(InstanceOid().profile.agent.iplocation.latitude, "lat") //
			.put(InstanceOid().profile.agent.iplocation.longitude, "lon") //
			.put(InstanceOid().profile.agent.iplocation.organization, "org") //
			.put(InstanceOid().profile.agent.iplocation.postal_code, "zip") //
			.put(InstanceOid().profile.agent.iplocation.region, "regionName") //
			.put(InstanceOid().profile.agent.iplocation.region_code, "region") //
			.put(InstanceOid().profile.agent.iplocation.timezone, "timezone") //
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
