//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
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
import com.sandpolis.core.instance.store.StoreConfig;
import com.sandpolis.core.net.MsgServer.RS_ServerBanner;
import com.sandpolis.server.vanilla.store.server.ServerStore.ServerStoreConfig;

/**
 * @author cilki
 * @since 5.0.0
 */
public final class ServerStore extends StoreBase<ServerStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(ServerStore.class);

	public ServerStore() {
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
	public ServerStore init(Consumer<ServerStoreConfig> configurator) {
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

		return (ServerStore) super.init(null);
	}

	public final class ServerStoreConfig extends StoreConfig {
	}

	public static final ServerStore ServerStore = new ServerStore();
}
