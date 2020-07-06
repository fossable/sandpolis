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
package com.sandpolis.core.instance;

import static java.util.UUID.randomUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.soi.Build.SO_Build;
import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.foundation.Platform;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;

/**
 * Contains common fields useful to every instance type.
 *
 * @author cilki
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
	public static final SO_Build SO_BUILD;

	/**
	 * The dependency matrix included in the instance jar.
	 */
	public static final SO_DependencyMatrix SO_MATRIX;

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
			SO_MATRIX = readMatrix();
			SO_BUILD = readBuild();

			// TODO set from PrefStore
			UUID = randomUUID().toString();
		} else {
			log.warn("Applying unit test configuration");

			INSTANCE = InstanceType.CHARCOAL;
			FLAVOR = InstanceFlavor.NONE;
			SO_BUILD = null;
			SO_MATRIX = null;
			UUID = randomUUID().toString();
		}
	}

	/**
	 * Get the instance's {@link SO_DependencyMatrix} object.
	 *
	 * @return The instance's {@link SO_DependencyMatrix} object
	 */
	private static SO_DependencyMatrix readMatrix() {
		if (MainDispatch.getMain() == null)
			throw new IllegalStateException("Core initialized before dispatch");

		try {
			return SO_DependencyMatrix.parseFrom(MainDispatch.getMain().getResourceAsStream("/soi/matrix.bin"));
		} catch (Exception e) {
			throw new RuntimeException("Failed to read SO_MATRIX!", e);
		}
	}

	/**
	 * Get the instance's {@link SO_Build} object.
	 *
	 * @return The instance's {@link SO_Build} object
	 */
	private static SO_Build readBuild() {
		if (MainDispatch.getMain() == null)
			throw new IllegalStateException("Core initialized before dispatch");

		try {
			return SO_Build.parseFrom(MainDispatch.getMain().getResourceAsStream("/soi/build.bin"));
		} catch (Exception e) {
			throw new RuntimeException("Failed to read SO_MATRIX!", e);
		}
	}

	private Core() {
	}
}
