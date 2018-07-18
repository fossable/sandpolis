/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.core.instance;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.proto.soi.Build.SO_Build;
import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.proto.util.Platform.Instance;

/**
 * Contains common fields useful to every instance type.
 * 
 * @author cilki
 * @since 2.0.0
 */
public final class Core {
	private Core() {
	}

	public static final Logger log = LoggerFactory.getLogger(Core.class);

	/**
	 * The instance type.
	 */
	public static final Instance INSTANCE;

	/**
	 * Build information included in the instance jar.
	 */
	public static final SO_Build SO_BUILD;

	/**
	 * The dependency matrix included in the instance jar.
	 */
	public static final SO_DependencyMatrix SO_MATRIX;

	static {
		try {
			INSTANCE = MainDispatch.getInstance();
		} catch (Exception e) {
			throw new RuntimeException("Failed to determine instance!", e);
		}

		try {
			SO_MATRIX = SO_DependencyMatrix.parseFrom(MainDispatch.getMain().getResourceAsStream("/soi/matrix.bin"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to read SO_MATRIX!", e);
		}

		try {
			SO_BUILD = SO_Build.parseFrom(MainDispatch.getMain().getResourceAsStream("/soi/build.bin"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to read SO_BUILD!", e);
		}
	}

	/**
	 * The instance's CVID.
	 */
	private static int cvid;

	/**
	 * The instance's UUID.
	 */
	private static String uuid;

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

	/**
	 * Get the UUID.
	 * 
	 * @return The instance's current UUID
	 */
	public static String uuid() {
		return uuid;
	}

	/**
	 * Set the instance's UUID.
	 * 
	 * @param uuid A new UUID
	 */
	public static void setUuid(String uuid) {
		if (uuid == null)
			throw new IllegalArgumentException();
		if (uuid.equals(Core.uuid))
			log.warn("Setting UUID to same value");

		Core.uuid = uuid;
	}

}
