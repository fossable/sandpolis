/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.vanilla.store.server;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Store;
import com.sandpolis.core.instance.Store.AutoInitializer;
import com.sandpolis.core.instance.store.pref.PrefStore;
import com.sandpolis.core.proto.net.MCServer.RS_ServerBanner;
import com.sandpolis.server.vanilla.ConfigConstant.server;

/**
 * @author cilki
 * @since 5.0.0
 */
@AutoInitializer
public final class ServerStore extends Store {

	private static final Logger log = LoggerFactory.getLogger(ServerStore.class);

	/**
	 * The cached server banner response.
	 */
	private static RS_ServerBanner banner;

	public static void init() {
		loadBanner();
	}

	static {
		init();
	}

	/**
	 * Load the server banner from storage.
	 */
	public static void loadBanner() {
		if (banner != null)
			log.debug("Reloading server banner");

		var ban = RS_ServerBanner.newBuilder().setVersion(Core.SO_BUILD.getVersion())
				.setBanner(Config.get(server.banner.text));

		String imagePath = Config.get(server.banner.image);
		if (imagePath != null) {
			try (var in = new FileInputStream(imagePath)) {
				ban.setBannerImage(ByteString.readFrom(in));
			} catch (IOException e) {
				log.error("Failed to read banner image", e);
			}
		} else {
			byte[] image = PrefStore.getBytes("banner.image.bytes");
			if (image != null)
				ban.setBannerImage(ByteString.copyFrom(image));
		}

		// Validate image format
		try (var in = new ByteArrayInputStream(ban.getBannerImage().toByteArray())) {
			ImageIO.read(in);
		} catch (IOException e) {
			log.error("Invalid banner image format", e);
			ban.clearBannerImage();
		}

		banner = ban.build();
	}

	/**
	 * Get the cached banner response.
	 * 
	 * @return The banner response
	 */
	public static RS_ServerBanner getBanner() {
		return banner;
	}

	private ServerStore() {
	}
}
