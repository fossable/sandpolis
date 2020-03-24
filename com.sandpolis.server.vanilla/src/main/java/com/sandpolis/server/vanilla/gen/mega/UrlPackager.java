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
package com.sandpolis.server.vanilla.gen.mega;

import com.google.common.io.BaseEncoding;
import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.server.vanilla.gen.MegaGen;

/**
 * This generator produces a URL with an embedded configuration.
 *
 * @author cilki
 * @since 5.0.0
 */
public class UrlPackager extends MegaGen {
	public UrlPackager(GenConfig config) {
		super(config, "url");
	}

	@Override
	protected byte[] generate() throws Exception {
		String url = "https://sandpolis.com/config?c=";

		switch (config.getPayload()) {
		case OUTPUT_CONFIG:
			switch (config.getPayloadConfigCase()) {
			case MEGA:
				url += BaseEncoding.base64Url().encode(config.getMega().toByteArray());
				break;
			case MICRO:
				url += BaseEncoding.base64Url().encode(config.getMicro().toByteArray());
				break;
			default:
				throw new RuntimeException();
			}
			break;
		}

		return url.getBytes();
	}
}
