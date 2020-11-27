package com.sandpolis.core.server.generator;

import java.lang.module.ResolvedModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.Platform.OsType;
import com.sandpolis.core.instance.Environment;

public class RuntimeCache {

	private static final Logger log = LoggerFactory.getLogger(RuntimeCache.class);

	private HttpClient client = HttpClient.newHttpClient();

	public Optional<Path> getRuntime(Module module, int majorVersion, OsType os) {

		// Compute modules
		module.getLayer().configuration().modules().stream().map(ResolvedModule::name).collect(Collectors.joining(","));

		String jlinkOs;
		switch (os) {
		case DARWIN:
			jlinkOs = "mac";
			break;
		case LINUX:
			jlinkOs = "linux";
			break;
		case WINDOWS:
			jlinkOs = "windows";
			break;
		default:
			throw new IllegalArgumentException();
		}

		// Check cache
		var destination = Environment.GEN.path().resolve(String.format("%d-%s", majorVersion, jlinkOs));
		if (Files.exists(destination)) {
			return Optional.of(destination);
		}

		try {
			var response = downloadRuntime(9, null).get();
			if (response.statusCode() == 200) {
				return Optional.of(response.body());
			} else {
				return Optional.empty();
			}
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public CompletableFuture<HttpResponse<Path>> downloadRuntime(int majorVersion, OsType os) {
		var url = URI.create(String.format("https://jlink.online/runtime/%s"));
		log.debug("Query URL: {}", url);

		Path destination = Environment.GEN.path().resolve("");

		return client.sendAsync(HttpRequest.newBuilder().uri(url).timeout(Duration.ofMinutes(2)).GET().build(),
				HttpResponse.BodyHandlers.ofFile(destination));
	}
}
