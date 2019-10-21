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
package com.sandpolis.plugin.sysinfo.client.mega;

import java.util.function.Function;

import com.google.protobuf.Message;
import com.sandpolis.core.instance.plugin.SandpolisPlugin;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.core.profile.AK_CLIENT;
import com.sandpolis.core.profile.attribute.key.AttributeKey;
import com.sandpolis.plugin.sysinfo.AK_CPU;
import com.sandpolis.plugin.sysinfo.client.mega.exe.SysinfoExe;
import com.sandpolis.plugin.sysinfo.net.MessageSysinfo.SysinfoMSG;

import oshi.hardware.CentralProcessor;
import oshi.software.os.NetworkParams;

public final class SysinfoPlugin extends SandpolisPlugin implements ExeletProvider {

	private void setupAttributes() {
		associate(AK_CPU.VENDOR, (CentralProcessor cpu) -> cpu.getVendor());
		associate(AK_CPU.MODEL, (CentralProcessor cpu) -> cpu.getModel());
		associate(AK_CLIENT.HOSTNAME, (NetworkParams net) -> net.getHostName());
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

	@Override
	public Class<? extends Message> getMessageType() {
		return SysinfoMSG.class;
	}
}
