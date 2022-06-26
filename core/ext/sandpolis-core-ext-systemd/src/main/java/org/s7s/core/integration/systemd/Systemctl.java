//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.systemd;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.foundation.S7SFile;
import org.s7s.core.foundation.S7SProcess;
import org.s7s.core.foundation.S7SSystem;

public record Systemctl(Path executable) {

	private static final Logger log = LoggerFactory.getLogger(Systemctl.class);

	public static boolean isAvailable() {
		switch (S7SSystem.OS_TYPE) {
		case LINUX:
			return S7SFile.which("systemctl").isPresent();
		default:
			return false;
		}
	}

	public static Systemctl load() {
		if (!isAvailable()) {
			throw new IllegalStateException();
		}

		return new Systemctl(S7SFile.which("systemctl").get().path());
	}

	public void enable(SystemdService service) {
		S7SProcess.exec(executable, "enable", service.path().getFileName().toString());
	}

	public void disable(SystemdService service) {
		S7SProcess.exec(executable, "disable", service.path().getFileName().toString());
	}

	public void start(SystemdService service) {
		S7SProcess.exec(executable, "start", service.path().getFileName().toString());
	}

	public void stop(SystemdService service) {
		S7SProcess.exec(executable, "stop", service.path().getFileName().toString());
	}

	public void restart(SystemdService service) {
		S7SProcess.exec(executable, "restart", service.path().getFileName().toString());
	}
}
