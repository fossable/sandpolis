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
package com.sandpolis.core.instance;

import com.github.cilki.tree_constants.TreeConstant;

public final class ConfigConstants {
	private ConfigConstants() {
	}

	/**
	 * Whether a Power-On Self Test will be executed on startup.
	 */
	@TreeConstant
	private static final String post = "post";

	/**
	 * Whether decoded messages will be logged.
	 */
	@TreeConstant
	private static final String logging_net_traffic_decoded = "logging.net.traffic.decoded";

	/**
	 * Whether raw network traffic will be logged.
	 */
	@TreeConstant
	private static final String logging_net_traffic_raw = "logging.net.traffic.raw";

	/**
	 * Whether the startup summary will be logged under normal circumstances.
	 */
	@TreeConstant
	private static final String logging_startup_summary = "logging.startup.summary";

	/**
	 * Whether TLS is enabled for all connections.
	 */
	@TreeConstant
	private static final String net_connection_tls = "net.connection.tls";

	/**
	 * The number of {@link Thread}s in the {@code net.connection.outgoing} pool.
	 */
	@TreeConstant
	private static final String net_connection_outgoing_pool__size = "net.connection.outgoing.pool_size";

	/**
	 * The number of {@link Thread}s in the {@code net.message.incoming} pool.
	 */
	@TreeConstant
	private static final String net_message_incoming_pool__size = "net.message.incoming.pool_size";

	/**
	 * The default message timeout in milliseconds.
	 */
	@TreeConstant
	private static final String net_message_default__timeout = "net.message.default_timeout";

	/**
	 * The number of {@link Thread}s in the {@code net.exelet} pool.
	 */
	@TreeConstant
	private static final String net_exelet_pool__size = "net.exelet.pool_size";

	/**
	 * Whether instance mutex will be checked with IPC.
	 */
	@TreeConstant
	private static final String net_ipc_mutex = "net.ipc.mutex";

	/**
	 * The IPC connection timeout.
	 */
	@TreeConstant
	private static final String net_ipc_timeout = "net.ipc.timeout";

	/**
	 * Whether plugins will be loaded.
	 */
	@TreeConstant
	private static final String plugin_enabled = "plugin.enabled";

	/**
	 * Whether plugins certificates will be strictly checked.
	 */
	@TreeConstant
	private static final String plugin_certificate_strict = "plugin.certificate.strict";

	/**
	 * The log directory.
	 */
	@TreeConstant
	private static final String path_log = "path.log";

	/**
	 * The tmp directory.
	 */
	@TreeConstant
	private static final String path_tmp = "path.tmp";

	/**
	 * The lib directory.
	 */
	@TreeConstant
	private static final String path_lib = "path.lib";

}
