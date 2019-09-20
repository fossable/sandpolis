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

import com.sandpolis.plugin.shell.client.mega.shell.BashShell;
import com.sandpolis.plugin.shell.client.mega.shell.CmdShell;
import com.sandpolis.plugin.shell.client.mega.shell.PwshShell;

public final class Shells {

	public static final PwshShell PWSH = new PwshShell();

	public static final CmdShell CMD = new CmdShell();

	public static final BashShell BASH = new BashShell();

	private Shells() {
	}
}
