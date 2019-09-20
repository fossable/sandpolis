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

import java.util.Base64;

public class BashShell extends AbstractShell {

	@Override
	public String[] searchPath() {
		return new String[] { "/bin/sh" };
	}

	@Override
	public String[] buildSession() {
		return new String[] { location };
	}

	@Override
	public String[] buildCommand(String command) {
		return new String[] { location, "-c",
				"echo " + Base64.getEncoder().encodeToString(command.getBytes()) + " | base64 --decode | " + location };
	}
}
