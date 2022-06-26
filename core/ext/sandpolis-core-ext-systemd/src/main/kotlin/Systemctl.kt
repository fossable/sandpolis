//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.systemd

import java.nio.file.Path

import org.s7s.core.foundation.S7SFile;
import org.s7s.core.foundation.S7SProcess;
import org.s7s.core.foundation.S7SSystem;

data class Systemctl(val executable: Path) {

	companion object {

		fun load() : Systemctl? {
			val exe = when (S7SSystem.OS_TYPE) {
				LINUX -> S7SFile.which("systemctl")
				else -> null
			}

			if (exe != null) {
				return Systemctl(exe.path)
			} else {
				return null
			}
		}
	}

	fun enable(service: SystemdService) {
		S7SProcess.exec(executable, "enable", service.path.fileName.toString());
	}

	fun disable(service: SystemdService) {
		S7SProcess.exec(executable, "disable", service.path.fileName.toString());
	}

	fun start(service: SystemdService) {
		S7SProcess.exec(executable, "start", service.path.fileName.toString());
	}

	fun stop(service: SystemdService) {
		S7SProcess.exec(executable, "stop", service.path.fileName.toString());
	}

	fun restart(service: SystemdService) {
		S7SProcess.exec(executable, "restart", service.path.fileName.toString());
	}
}
