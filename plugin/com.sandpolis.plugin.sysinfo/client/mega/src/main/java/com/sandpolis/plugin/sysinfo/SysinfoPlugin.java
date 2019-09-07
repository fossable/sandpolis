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
package com.sandpolis.plugin.sysinfo;

import java.util.function.Function;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.instance.plugin.ExeletProvider;
import com.sandpolis.core.net.command.Exelet;

import oshi.hardware.CentralProcessor;
import oshi.software.os.NetworkParams;

public class SysinfoPlugin extends Plugin implements ExeletProvider {

	public SysinfoPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public void start() {
		setupAttributes();
		super.start();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
	}

	private void setupAttributes() {
		associate(AK_CPU.VENDOR, (CentralProcessor cpu) -> cpu.getVendor());
		associate(AK_CPU.MODEL, (CentralProcessor cpu) -> cpu.getModel());
		associate(AK_NET.HOSTNAME, (NetworkParams net) -> net.getHostName());
	}

	/**
	 * Associate the given key with the given value retriever.
	 *
	 * @param key       An attribute key
	 * @param retriever A value retriever
	 */
	private <T> void associate(AttributeKey<T> key, Function<?, T> retriever) {
		key.putObject("retriever", retriever);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Exelet>[] getExelets() {
		return new Class[] { SysinfoExe.class };
	}

}
