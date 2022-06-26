//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.systemd;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.s7s.core.foundation.S7SProcess;

public record SystemdLog(Path log) {

	public Stream<?> stream() throws IOException {
		new ObjectMapper().createParser(S7SProcess.exec("journalctl", "-o", "json").process().getInputStream());
		return null;
	}
}
