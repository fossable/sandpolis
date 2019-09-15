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
package com.sandpolis.plugin.sysinfo.client.mega.exe;

import com.google.protobuf.Message;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.plugin.sysinfo.net.MCSysinfo.RQ_NicTotals;
import com.sandpolis.plugin.sysinfo.net.MCSysinfo.RS_NicTotals;
import com.sandpolis.plugin.sysinfo.net.MSG;

import oshi.SystemInfo;

public class SysinfoExe extends Exelet {

	@Auth
	@Handler(tag = MSG.SysinfoMessage.RQ_NIC_TOTALS_FIELD_NUMBER)
	public Message.Builder rq_nic_totals(RQ_NicTotals rq) {

		long upload = 0;
		long download = 0;

		for (var nif : new SystemInfo().getHardware().getNetworkIFs()) {
			download += nif.getBytesRecv();
			upload += nif.getBytesSent();
		}

		return RS_NicTotals.newBuilder().setUpload(upload).setDownload(download);
	}
}
