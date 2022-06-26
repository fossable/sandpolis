//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.location;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.server.ServerContext;

/**
 * An {@link AbstractGeolocationService} resolves a set of location attributes
 * for an IP address. The service may use HTTP, HTTPS, or a local file for
 * resolution.
 *
 * @since 5.1.1
 */
public abstract class AbstractGeolocationService {

	private static final Logger log = LoggerFactory.getLogger(AbstractGeolocationService.class);

	private HttpClient client;

	/**
	 * The application protocol which may be 'http', 'https', or 'file'.
	 */
	protected final String protocol;

	/**
	 * The request timeout.
	 */
	private Duration timeout = Duration.ofSeconds(ServerContext.GEOLOCATION_TIMEOUT.get());

	protected AbstractGeolocationService(String protocol) {
		this.protocol = Objects.requireNonNull(protocol).toLowerCase();
		switch (this.protocol) {
		case "http":
			log.info("Using an insecure geolocation service");
		case "https":
			client = HttpClient.newHttpClient();
			break;
		case "file":
			break;
		default:
			throw new IllegalArgumentException("Unknown protocol");
		}
	}

	/**
	 * Build a geolocation query for the given IP address and location attributes.
	 *
	 * @param ip     The IP address
	 * @param fields The desired attributes from {@link IpLocation}
	 * @return The query
	 */
	protected abstract String buildQuery(String ip, Oid... fields);

	/**
	 * Convert the query result into a {@link IpLocation} object.
	 *
	 * @param result The query result
	 * @return The location
	 * @throws Exception
	 */
	protected abstract IpLocation parseLocation(String result) throws Exception;

	public CompletableFuture<IpLocation> query(String ip, Oid... fields) {
		var url = URI.create(buildQuery(ip, fields));
		log.debug("Query URL: {}", url);

		HttpRequest request = HttpRequest.newBuilder().uri(url).timeout(timeout).GET().build();

		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApplyAsync(rs -> {
			try {
				return parseLocation(rs.body());
			} catch (Exception e) {
				log.debug("Query failed", e);
				throw new CompletionException(e);
			}
		});
	}
}
