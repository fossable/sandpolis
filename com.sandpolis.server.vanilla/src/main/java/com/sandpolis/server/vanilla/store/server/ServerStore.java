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

import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.sandpolis.core.instance.Config;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.proto.net.MCServer.RS_ServerBanner;
import com.sandpolis.server.vanilla.ConfigConstant.server;
import com.sandpolis.server.vanilla.store.server.ServerStore.ServerStoreConfig;

/**
 * @author cilki
 * @since 5.0.0
 */
public final class ServerStore extends StoreBase<ServerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ServerStore.class);

	/**
	 * The cached server banner response.
	 */
	private RS_ServerBanner banner;

	/**
	 * Get the cached banner response.
	 * 
	 * @return The banner response
	 */
	public RS_ServerBanner getBanner() {
		return banner;
	}

	@Override
	public ServerStore init(Consumer<ServerStoreConfig> configurator) {
		var config = new ServerStoreConfig();
		configurator.accept(config);

		if (banner != null)
			log.debug("Reloading server banner");

		var b = RS_ServerBanner.newBuilder().setVersion(Core.SO_BUILD.getVersion())
				.setBanner(Config.get(server.banner.text));

		String imagePath = Config.get(server.banner.image);
		if (imagePath != null) {
			try (var in = new FileInputStream(imagePath)) {
				b.setBannerImage(ByteString.readFrom(in));
			} catch (IOException e) {
				log.error("Failed to read banner image", e);
			}
		} else {
			byte[] image = PrefStore.getBytes("banner.image.bytes");
			if (image != null)
				b.setBannerImage(ByteString.copyFrom(image));
		}

		// Validate image format
		try (var in = new ByteArrayInputStream(b.getBannerImage().toByteArray())) {
			ImageIO.read(in);
		} catch (IOException e) {
			log.error("Invalid banner image format", e);
			b.clearBannerImage();
		}

		banner = b.build();

		return (ServerStore) super.init(null);
	}

	public final class ServerStoreConfig extends StoreConfig {
	}

	public static final ServerStore ServerStore = new ServerStore();
}
