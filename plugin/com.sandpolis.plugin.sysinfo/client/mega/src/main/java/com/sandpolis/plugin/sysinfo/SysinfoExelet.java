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

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MSG.Message;

public class SysinfoExelet extends Exelet {

	public SysinfoExelet(Sock connector) {
		super(connector);
	}

	@Auth
	public void rq_query(Message m) {
		// TODO
	}

	@Auth
	public void rq_enumerate(Message m) {
		// TODO
	}

}
