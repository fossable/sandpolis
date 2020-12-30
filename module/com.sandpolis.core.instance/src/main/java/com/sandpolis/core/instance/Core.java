//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance;

import static java.util.UUID.randomUUID;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;

/**
 * Contains common fields useful to every instance type.
 *
 * @since 2.0.0
 */
public final class Core {

	public static final Logger log = LoggerFactory.getLogger(Core.class);

	/**
	 * The instance type.
	 */
	public static final InstanceType INSTANCE;

	/**
	 * The instance subtype.
	 */
	public static final InstanceFlavor FLAVOR;

	/**
	 * Build information included in the instance jar.
	 */
	public static final Properties SO_BUILD;

	/**
	 * The instance's UUID.
	 */
	public static final String UUID;

	/**
	 * The instance's CVID.
	 */
	private static int cvid;

	/**
	 * Get the CVID.
	 *
	 * @return The instance's current CVID
	 */
	public static int cvid() {
		return cvid;
	}

	/**
	 * Set the instance's CVID.
	 *
	 * @param cvid A new CVID
	 */
	public static void setCvid(int cvid) {
		if (cvid == Core.cvid)
			log.warn("Setting CVID to same value");

		Core.cvid = cvid;
	}

	static {
		if (MainDispatch.getInstance() != null && MainDispatch.getInstanceFlavor() != null) {
			INSTANCE = MainDispatch.getInstance();
			FLAVOR = MainDispatch.getInstanceFlavor();

			// TODO set from PrefStore
			UUID = randomUUID().toString();
		} else {
			log.warn("Applying unit test configuration");

			INSTANCE = InstanceType.CHARCOAL;
			FLAVOR = InstanceFlavor.NONE;
			UUID = randomUUID().toString();
		}

		SO_BUILD = new Properties();
		try {
			SO_BUILD.load(MainDispatch.getMain().getResourceAsStream("/build.properties"));
		} catch (IOException e) {
			log.warn("Failed to load SO_BUILD");
		}
	}

	private Core() {
	}
}
