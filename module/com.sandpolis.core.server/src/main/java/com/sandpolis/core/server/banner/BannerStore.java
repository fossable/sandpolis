//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.banner;

import static com.sandpolis.core.instance.pref.PrefStore.PrefStore;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.foundation.ConfigStruct;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.store.ConfigurableStore;
import com.sandpolis.core.instance.store.StoreBase;
import com.sandpolis.core.server.banner.BannerStore.ServerStoreConfig;
import com.sandpolis.core.clientserver.msg.MsgServer.RS_ServerBanner;

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
		var config = new ServerStoreConfig();
		configurator.accept(config);

		if (banner != null)
			log.debug("Reloading server banner");

		var b = RS_ServerBanner.newBuilder().setVersion(Core.SO_BUILD.getVersion())
				.setBanner(Config.BANNER_TEXT.value().orElse("Sandpolis Server"));

		Config.BANNER_IMAGE.value().ifPresentOrElse(path -> {
			try (var in = new FileInputStream(path)) {
				b.setBannerImage(ByteString.readFrom(in));
			} catch (IOException e) {
				log.error("Failed to read banner image", e);
			}
		}, () -> {
			byte[] image = PrefStore.getBytes("banner.image.bytes");
			if (image != null)
				b.setBannerImage(ByteString.copyFrom(image));
		});

		// Validate image format
		try (var in = new ByteArrayInputStream(b.getBannerImage().toByteArray())) {
			ImageIO.read(in);
		} catch (IOException e) {
			log.error("Invalid banner image format", e);
			b.clearBannerImage();
		}

		banner = b.build();
	}

	@ConfigStruct
	public static final class ServerStoreConfig {
	}

	public static final BannerStore BannerStore = new BannerStore();
}
