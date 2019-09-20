/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.plugin.shell.client.mega.shell;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PwshShell extends AbstractShell {

	@Override
	public String[] searchPath() {
		return new String[] { "/usr/bin/pwsh", "C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe",
				"C:/Windows/SysWOW64/WindowsPowerShell/v1.0/powershell.exe" };
	}

	@Override
	public String[] buildSession() {
		return new String[] { location };
	}

	@Override
	public String[] buildCommand(String command) {
		return new String[] { location, "-encodedCommand",
				Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_16LE)) };
	}
}
