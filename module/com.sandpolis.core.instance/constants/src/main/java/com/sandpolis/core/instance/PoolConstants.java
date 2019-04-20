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
package com.sandpolis.core.instance;

import com.github.cilki.tree_constants.TreeConstant;

public final class PoolConstants {
	private PoolConstants() {
	}

	/**
	 * The {@link ExecutorService} for outgoing connection attempts.
	 */
	@TreeConstant
	private static final String net_connection_outgoing = "net.connection.outgoing";

	/**
	 * The {@link ExecutorService} for incoming messages.
	 */
	@TreeConstant
	private static final String net_message_incoming = "net.message.incoming";

	/**
	 * The {@link ExecutorService} that runs message handlers.
	 */
	@TreeConstant
	private static final String net_exelet = "net.exelet";

	/**
	 * The {@link ExecutorService} that runs DNS queries.
	 */
	@TreeConstant
	private static final String net_dns_resolver = "net.dns.resolver";

	/**
	 * The {@link ExecutorService} that delivers application events.
	 */
	@TreeConstant
	private static final String signaler = "signaler";

}
