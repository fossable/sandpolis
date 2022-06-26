//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.launchd;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.foundation.S7SFile;
import org.s7s.core.foundation.S7SSystem;

public record Launchctl(Path executable) {

	private static final Logger log = LoggerFactory.getLogger(Launchctl.class);

	public static boolean isAvailable() {
		switch (S7SSystem.OS_TYPE) {
		case MACOS:
			return S7SFile.which("launchctl").isPresent();
		default:
			return false;
		}
	}

	public static Launchctl load() {
		if (!isAvailable()) {
			throw new IllegalStateException();
		}

		return new Launchctl(S7SFile.which("launchctl").get().path());
	}
}
