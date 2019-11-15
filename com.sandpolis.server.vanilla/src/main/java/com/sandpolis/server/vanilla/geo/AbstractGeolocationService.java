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

import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.proto.util.LocationOuterClass.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractGeolocationService {

	private static final Logger log = LoggerFactory.getLogger(AbstractGeolocationService.class);

	public static AbstractGeolocationService INSTANCE;

	private static final HttpClient client = HttpClient.newHttpClient();

	protected String protocol;

	private int timeout = 5;

	protected AbstractGeolocationService(String protocol) {
		this.protocol = Objects.requireNonNull(protocol);
		if (protocol.equals("http")) {
			log.info("Using an insecure geolocation service");
		}
	}

	protected abstract String buildQuery(String ip, Set<Integer> fields);

	protected abstract Location parseResult(String body) throws Exception;

	protected abstract ImmutableBiMap<Integer, String> getMapping();

	public Location query(String ip) throws IOException, InterruptedException {
		return query(ip, getMapping().keySet());
	}

	public Location query(String ip, Set<Integer> fields) throws IOException, InterruptedException {
		var url = URI.create(buildQuery(ip, fields));
		log.debug("Query URL: {}", url);

		HttpRequest request = HttpRequest.newBuilder().uri(url).timeout(Duration.ofSeconds(timeout)).GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println("status: " + response.statusCode());
		if (response.statusCode() == 200) {
			try {
				return parseResult(response.body());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
