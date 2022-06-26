//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.homebrew;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.s7s.core.foundation.S7SFile;
import org.s7s.core.foundation.S7SProcess;
import org.s7s.core.foundation.S7SSystem;

public record Brew(Path executable) {

	public static boolean isAvailable() {
		switch (S7SSystem.OS_TYPE) {
		case MACOS:
			return S7SFile.which("brew").isPresent();
		default:
			return false;
		}
	}

	public static Brew load() {
		if (!isAvailable()) {
			throw new IllegalStateException();
		}

		return new Brew(S7SFile.which("brew").get().path());
	}

	public S7SProcess install(String... packages) {
		return S7SProcess.exec("brew", "install", Arrays.stream(packages).collect(Collectors.joining(" ")));
	}

}
