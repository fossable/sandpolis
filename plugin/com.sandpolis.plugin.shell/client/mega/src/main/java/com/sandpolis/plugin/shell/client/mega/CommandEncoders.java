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
package com.sandpolis.plugin.shell.client.mega;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CommandEncoders {

	public static final class BashEncoder {
		public static String[] encode(String command) {
			return new String[] { "sh", "-c",
					"echo " + Base64.getEncoder().encodeToString(command.getBytes()) + " | base64 --decode | sh" };
		}
	}

	public static final class PowerShellEncoder {
		public static String[] encode(String command) {
			return new String[] { "powershell", "-encodedCommand",
					Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_16LE)) };
		}
	}
}
