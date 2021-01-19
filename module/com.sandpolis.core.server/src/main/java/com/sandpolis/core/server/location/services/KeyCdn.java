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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.instance.state.IpLocationOid;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.server.location.AbstractGeolocationService;
import com.sandpolis.core.server.location.IpLocation;

/**
 * This {@link AbstractGeolocationService} implementation interacts with
 * <a href="https://tools.keycdn.com/geo">https://tools.keycdn.com/geo</a>.
 */
public final class KeyCdn extends AbstractGeolocationService {

	/**
	 * The fields provided by the location service associated with {@link Oid}s.
	 */
	private static final ImmutableBiMap<Oid, String> JSON_FIELDS = new ImmutableBiMap.Builder<Oid, String>()
			.put(InstanceOid().profile.agent.iplocation.as_code, "asn") //
			.put(InstanceOid().profile.agent.iplocation.city, "city") //
			.put(InstanceOid().profile.agent.iplocation.continent, "continent_name") //
			.put(InstanceOid().profile.agent.iplocation.continent_code, "continent_code") //
			.put(InstanceOid().profile.agent.iplocation.country, "country_name") //
			.put(InstanceOid().profile.agent.iplocation.country_code, "country_code") //
			.put(InstanceOid().profile.agent.iplocation.isp, "isp") //
			.put(InstanceOid().profile.agent.iplocation.latitude, "latitude") //
			.put(InstanceOid().profile.agent.iplocation.longitude, "lonitude") //
			.put(InstanceOid().profile.agent.iplocation.metro_code, "metro_code") //
			.put(InstanceOid().profile.agent.iplocation.postal_code, "postal_code") //
			.put(InstanceOid().profile.agent.iplocation.region, "region_name") //
			.put(InstanceOid().profile.agent.iplocation.region_code, "region_code") //
			.put(InstanceOid().profile.agent.iplocation.timezone, "timezone") //
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
	protected IpLocation parseLocation(String result) throws Exception {
		IpLocation location = new IpLocation(null);
		new ObjectMapper().readTree(result).path("data").path("geo").forEach(node -> {
			node.fields().forEachRemaining(entry -> {
				switch (entry.getKey()) {
				case "asn":
					location.set(IpLocationOid.AS_CODE, entry.getValue().asInt());
					break;
				case "city":
					location.set(IpLocationOid.CITY, entry.getValue().asText());
					break;
				case "continent_name":
					location.set(IpLocationOid.CONTINENT, entry.getValue().asText());
					break;
				case "continent_code":
					location.set(IpLocationOid.CONTINENT_CODE, entry.getValue().asText());
					break;
				case "country_name":
					location.set(IpLocationOid.COUNTRY, entry.getValue().asText());
					break;
				case "country_code":
					location.set(IpLocationOid.COUNTRY_CODE, entry.getValue().asText());
					break;
				case "isp":
					location.set(IpLocationOid.ISP, entry.getValue().asText());
					break;
				case "latitude":
					location.set(IpLocationOid.LATITUDE, entry.getValue().asDouble());
					break;
				case "longitude":
					location.set(IpLocationOid.LONGITUDE, entry.getValue().asDouble());
					break;
				case "metro_code":
					location.set(IpLocationOid.METRO_CODE, entry.getValue().asInt());
					break;
				case "postal_code":
					location.set(IpLocationOid.POSTAL_CODE, entry.getValue().asText());
					break;
				case "region_name":
					location.set(IpLocationOid.REGION, entry.getValue().asText());
					break;
				case "region_code":
					location.set(IpLocationOid.REGION_CODE, entry.getValue().asText());
					break;
				case "timezone":
					location.set(IpLocationOid.TIMEZONE, entry.getValue().asText());
					break;
				}
			});
		});

		return location;
	}
}
