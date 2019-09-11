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
package com.sandpolis.installer.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public final class CloudUtil {

	public static final class ListenResult {

		public final int status;

		public final String config;

		private ListenResult(int status) {
			this.status = status;
			this.config = null;
		}

		private ListenResult(String config) {
			this.status = 200;
			this.config = config;
		}
	}

	public static ListenResult listen(String token) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.sandpolis.com/v1/cloud/client/listen")).timeout(Duration.ofSeconds(30))
				.POST(BodyPublishers.ofString("{token: \"" + token + "\"}")).build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			return new ListenResult(response.statusCode());
		}

		return new ListenResult(response.body());
	}

	private CloudUtil() {
	}
}
