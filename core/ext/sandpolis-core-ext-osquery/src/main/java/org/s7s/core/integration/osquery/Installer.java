//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.osquery;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.s7s.core.foundation.Platform.ArchType;
import org.s7s.core.foundation.Platform.OsType;
import org.s7s.core.foundation.S7SFile;
import org.s7s.core.foundation.S7SProcess;
import org.s7s.core.foundation.S7SSystem;
import org.s7s.core.integration.osquery.Installer.Response.Asset;
import org.s7s.core.integration.pacman.Pacman;

public class Installer {

	public record Response(Asset[] assets) {
		public record Asset(String browser_download_url) {

		}
	}

	public static Optional<Path> locate() {
		// Try to find binary on PATH
		var bin = S7SFile.which("osqueryi");
		if (bin.isPresent()) {
			return Optional.of(bin.get().path());
		}

		return Optional.empty();
	}

	public static void install() throws IOException, InterruptedException {
		if (locate().isPresent()) {
			return;
		}

		// Attempt to install with a package manager first
		if (Pacman.isAvailable()) {
			if (Pacman.load().install("osquery").complete() == 0) {
				return;
			}
		}

		// Attempt manual installation
		var url = getLatestUrl(S7SSystem.OS_TYPE, S7SSystem.ARCH_TYPE);
		if (url.isPresent()) {
			// Download to tmp
			var tmp = Files.createTempFile(null, null);

			HttpClient.newHttpClient().send(HttpRequest.newBuilder()
					.uri(URI.create("https://api.github.com/repos/osquery/osquery/releases/latest")).GET().build(),
					BodyHandlers.ofFile(tmp));

			switch (S7SSystem.OS_TYPE) {
			case LINUX:
				break;
			case MACOS:
				break;
			case WINDOWS:
				// Execute the installer
				S7SProcess.exec(tmp);
				break;
			default:
				break;
			}
		}
	}

	public static Optional<String> getLatestUrl(OsType os, ArchType arch) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/repos/osquery/osquery/releases/latest")).GET().build();

		var response = new ObjectMapper()
				.createParser(HttpClient.newHttpClient().send(request, BodyHandlers.ofInputStream()).body())
				.readValueAs(Response.class);

		for (var asset : response.assets()) {
			switch (os) {
			case LINUX:
				if (asset.browser_download_url().contains("linux")) {
					switch (arch) {
					case X86_64:
						if (asset.browser_download_url().contains("x86_64.tar.gz")) {
							return Optional.of(asset.browser_download_url());
						}
						break;
					case AARCH64:
						if (asset.browser_download_url().contains("aarch64.tar.gz")) {
							return Optional.of(asset.browser_download_url());
						}
						break;
					default:
						break;
					}
				}
				break;
			case MACOS:
				if (asset.browser_download_url().contains("macos")) {
					switch (arch) {
					case X86_64:
						if (asset.browser_download_url().contains("x86_64.tar.gz")) {
							return Optional.of(asset.browser_download_url());
						}
						break;
					default:
						break;
					}
				}
				break;
			case WINDOWS:
				if (asset.browser_download_url().endsWith(".msi")) {
					switch (arch) {
					case X86_64:
						return Optional.of(asset.browser_download_url());
					default:
						break;
					}
				}
				break;
			default:
				break;
			}
		}

		return Optional.empty();
	}
}
