//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.installer.java.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CloudUtil {

	private static final Logger log = LoggerFactory.getLogger(CloudUtil.class);

	public static Optional<String> listen(String token) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.sandpolis.com/v1/cloud/agent/listen")).timeout(Duration.ofSeconds(30))
				.POST(BodyPublishers.ofString("{\"token\": \"" + token + "\"}")).build();
		log.debug("Request token: {}", token);

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			return Optional.empty();
		}

		if (response.body().contains("\"success\":true")) {
			Matcher match = Pattern.compile("\"config\":\"(.*)\"").matcher(response.body());
			if (match.find())
				return Optional.of(match.group(1));
		}

		return Optional.empty();
	}

	private CloudUtil() {
	}
}
