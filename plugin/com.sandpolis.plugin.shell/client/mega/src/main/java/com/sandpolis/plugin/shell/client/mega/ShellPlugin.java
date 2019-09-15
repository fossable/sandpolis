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

import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.sandpolis.core.instance.plugin.ExeletProvider;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.plugin.shell.client.mega.exe.ShellExe;

public class ShellPlugin extends Plugin {

	public ShellPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static final class Exelets implements ExeletProvider {
		@Override
		@SuppressWarnings("unchecked")
		public Class<? extends Exelet>[] getExelets() {
			return new Class[] { ShellExe.class };
		}
	}
}
