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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;
import com.sandpolis.core.instance.LocationOuterClass.Location;

/**
 * A geolocation service resolves a set of location attributes for an IP
 * address. The service may use HTTP, HTTPS, or a local file for resolution.
 *
 * @since 5.1.1
 */
public abstract class AbstractGeolocationService {

	private static final Logger log = LoggerFactory.getLogger(AbstractGeolocationService.class);

	/**
	 * Maps tag numbers from {@link Location} to field names specific to the
	 * particular service.
	 */
	public final ImmutableBiMap<Integer, String> attrMap;

	/**
	 * The application protocol which may be 'http', 'https', or 'file'.
	 */
	protected final String protocol;

	private int timeout = 5;

	protected AbstractGeolocationService(ImmutableBiMap<Integer, String> attrMap, String protocol) {
		this.attrMap = Objects.requireNonNull(attrMap);
		this.protocol = Objects.requireNonNull(protocol);
		if (protocol.equals("http")) {
			log.info("Using an insecure geolocation service");
		}
	}

	/**
	 * Build a geolocation query for the given IP address and location attributes.
	 *
	 * @param ip     The IP address
	 * @param fields The desired attributes from {@link Location}
	 * @return The query
	 */
	protected abstract String buildQuery(String ip, Set<Integer> fields);

	/**
	 * Convert the query result into a {@link Location} object.
	 *
	 * @param result The query result
	 * @return The location
	 * @throws Exception
	 */
	protected abstract Location parseLocation(String result) throws Exception;

	private HttpClient client = HttpClient.newHttpClient();

	public CompletableFuture<Location> query(String ip, Set<Integer> fields) {
		var url = URI.create(buildQuery(ip, fields));
		log.debug("Query URL: {}", url);

		HttpRequest request = HttpRequest.newBuilder().uri(url).timeout(Duration.ofSeconds(timeout)).GET().build();

		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApplyAsync(rs -> {
			try {
				return parseLocation(rs.body());
			} catch (Exception e) {
				log.debug("Query failed", e);
				return null;
			}
		});
	}
}
