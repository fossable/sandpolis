/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.plugin.sysinfo;

import java.util.function.Function;

import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.instance.store.plugin.SandpolisPlugin;

import oshi.hardware.CentralProcessor;
import oshi.software.os.NetworkParams;

public class SysinfoPlugin extends Plugin {

	public SysinfoPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class Test implements SandpolisPlugin {
		
		public Test() {}

		@Override
		public void load() {
			System.out.println("Loading plugin");
			registerAttributes();
		}

		public void registerAttributes() {
			register(AK_CPU.VENDOR, (CentralProcessor cpu) -> cpu.getVendor());
			register(AK_NET.HOSTNAME, (NetworkParams net) -> net.getHostName());
		}

		private <T> void register(AttributeKey<T> key, Function<?, T> fetch) {
			key.putObject("fetcher", fetch);
		}

		@Override
		public void unload() {
			// TODO Auto-generated method stub

		}
	}
}
