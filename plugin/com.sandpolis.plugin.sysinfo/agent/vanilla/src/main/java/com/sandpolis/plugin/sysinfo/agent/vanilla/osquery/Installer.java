package com.sandpolis.plugin.sysinfo.agent.vanilla.osquery;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.Optional;

import com.sandpolis.core.foundation.Platform.OsType;

public class Installer {

	public static class ReleaseAsset {

		public String browser_download_url;
	}

	public static Optional<Path> getBinary() {

		return Optional.empty();
	}

	public static void download(OsType os, String version) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/repos/osquery/osquery/releases/latest")).GET().build();

		HttpClient.newHttpClient().send(request, BodyHandlers.ofByteArray());
	}
}
