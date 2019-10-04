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
package com.sandpolis.plugin.desktop.client.mega;

import com.google.protobuf.Message;
import com.sandpolis.core.instance.plugin.SandpolisPlugin;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.plugin.ExeletProvider;
import com.sandpolis.plugin.desktop.client.mega.exe.DesktopExe;
import com.sandpolis.plugin.desktop.net.MSG;

public final class DesktopPlugin extends SandpolisPlugin implements ExeletProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Exelet>[] getExelets() {
		return new Class[] { DesktopExe.class };
	}

	@Override
	public Class<? extends Message> getMessageType() {
		return MSG.DesktopMessage.class;
	}

}
