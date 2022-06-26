//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.s7s.core.server.ServerConfig.BannerCfg;
import org.s7s.core.server.ServerConfig.GeolocationCfg;
import org.s7s.core.server.ServerConfig.StorageCfg;
import org.s7s.core.server.ServerConfig.StorageCfg.MongoCfg;

public record ServerConfig(GeolocationCfg geolocation, BannerCfg banner, StorageCfg storage) {

	private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

	public static final ServerConfig EMBEDDED = load();

	private static ServerConfig load() {

		try (var in = ServerConfig.class.getResourceAsStream("/org.s7s.core.server.json")) {
			if (in != null) {
				return new ObjectMapper().readValue(in, ServerConfig.class);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return null;
	}

	public record GeolocationCfg(

			/**
			 * The service to use for geolocation requests.
			 */
			String service,

			/**
			 * The geolocation API key.
			 */
			String key,

			/**
			 * The geolocation request timeout.
			 */
			int timeout) {

	}

	public record BannerCfg(

			/**
			 * Whether a server banner will be sent to prospective connections.
			 */
			boolean enabled,

			/**
			 * A path to an image to use in the server banner.
			 */
			String image,

			/**
			 * A greeting message to use in the server banner.
			 */
			String text) {
	}

	public record StorageCfg(String provider, MongoCfg mongodb) {
		public record MongoCfg(

				/**
				 * The MongoDB endpoint address and port.
				 */
				String host,

				/**
				 * The MongoDB database username.
				 */
				String username,

				/**
				 * The MongoDB database password.
				 */
				String password) {

		}
	}
}
