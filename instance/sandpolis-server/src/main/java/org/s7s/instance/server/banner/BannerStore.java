//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.banner;

import static org.s7s.core.instance.pref.PrefStore.PrefStore;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import org.s7s.core.protocol.Server.RS_ServerBanner;
import org.s7s.core.instance.BuildConfig;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.StoreBase;
import org.s7s.core.server.ServerContext;
import org.s7s.core.server.banner.BannerStore.ServerStoreConfig;

/**
 * {@link BannerStore} manages the server banner which is presented to new
 * connections.
 *
 * @since 5.0.0
 */
public final class BannerStore extends StoreBase implements ConfigurableStore<ServerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(BannerStore.class);

	public BannerStore() {
		super(log);
	}

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
	public void init(Consumer<ServerStoreConfig> configurator) {
		var config = new ServerStoreConfig(configurator);

		if (banner != null)
			log.debug("Reloading server banner");

		var b = RS_ServerBanner.newBuilder().setMessage(ServerContext.BANNER_TEXT.get());

		if (BuildConfig.EMBEDDED != null) {
			b.setVersion(BuildConfig.EMBEDDED.versions().instance());
		}

		if (ServerContext.BANNER_IMAGE.get() != null) {
			try (var in = new FileInputStream(ServerContext.BANNER_IMAGE.get())) {
				b.setImage(ByteString.readFrom(in));
			} catch (IOException e) {
				log.error("Failed to read banner image", e);
			}
		} else {
			byte[] image = PrefStore.getBytes("banner.image.bytes");
			if (image != null)
				b.setImage(ByteString.copyFrom(image));
		}

		// Validate image format
		try (var in = new ByteArrayInputStream(b.getImage().toByteArray())) {
			ImageIO.read(in);
		} catch (IOException e) {
			log.error("Invalid banner image format", e);
			b.clearImage();
		}

		banner = b.build();
	}

	public static final class ServerStoreConfig {
		private ServerStoreConfig(Consumer<ServerStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final BannerStore BannerStore = new BannerStore();
}
