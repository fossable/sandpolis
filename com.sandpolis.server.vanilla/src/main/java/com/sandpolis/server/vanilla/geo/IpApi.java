/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.server.vanilla.geo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.proto.util.LocationOuterClass.Location;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class IpApi extends AbstractGeolocationService {

	private static final ImmutableBiMap<Integer, String> mapping = new ImmutableBiMap.Builder<Integer, String>()
			.put(Location.AS_FIELD_NUMBER, "as").put(Location.CITY_FIELD_NUMBER, "city")
			.put(Location.CONTINENT_FIELD_NUMBER, "continent")
			.put(Location.CONTINENT_CODE_FIELD_NUMBER, "continentCode").put(Location.COUNTRY_FIELD_NUMBER, "country")
			.put(Location.COUNTRY_CODE_FIELD_NUMBER, "countryCode").put(Location.CURRENCY_FIELD_NUMBER, "currency")
			.put(Location.DISTRICT_FIELD_NUMBER, "district").put(Location.ISP_FIELD_NUMBER, "isp")
			.put(Location.LATITUDE_FIELD_NUMBER, "lat").put(Location.LONGITUDE_FIELD_NUMBER, "lon")
			.put(Location.ORGANIZATION_FIELD_NUMBER, "org").put(Location.POSTAL_CODE_FIELD_NUMBER, "zip")
			.put(Location.REGION_FIELD_NUMBER, "regionName").put(Location.REGION_CODE_FIELD_NUMBER, "region")
			.put(Location.TIMEZONE_FIELD_NUMBER, "timezone").build();

	@Override
	protected ImmutableBiMap<Integer, String> getMapping() {
		return mapping;
	}

	private String key;

	public IpApi() {
		super("http");
	}

	public IpApi(String key) {
		super("https");
		this.key = Objects.requireNonNull(key);
	}

	public Location query(String ip) throws IOException, InterruptedException {
		return query(ip, mapping.keySet());
	}

	@Override
	protected String buildQuery(String ip, Set<Integer> fields) {
		return String.format("%s://ip-api.com/json/%s?fields=%s", protocol, ip,
				fields.stream().map(mapping::get).collect(Collectors.joining(",")));
	}

	@Override
	protected Location parseResult(String body) throws Exception {
		var location = Location.newBuilder();
		new ObjectMapper().readTree(body).fields().forEachRemaining(entry -> {
			var field = Location.getDescriptor().findFieldByNumber(mapping.inverse().get(entry.getKey()));
			switch (field.getNumber()) {
			case Location.LATITUDE_FIELD_NUMBER:
			case Location.LONGITUDE_FIELD_NUMBER:
				// Set double value
				location.setField(field, entry.getValue().asDouble());
				break;
			case Location.POSTAL_CODE_FIELD_NUMBER:
				// Set int value
				location.setField(field, entry.getValue().asInt());
				break;
			case Location.MOBILE_FIELD_NUMBER:
				// Set boolean value
				location.setField(field, entry.getValue().asBoolean());
				break;
			default:
				// Set string value
				location.setField(field, entry.getValue().asText());
				break;
			}
		});

		return location.build();
	}
}
