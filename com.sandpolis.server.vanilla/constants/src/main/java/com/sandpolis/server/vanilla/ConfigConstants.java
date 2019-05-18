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
package com.sandpolis.server.vanilla;

import com.github.cilki.tree_constants.TreeConstant;

public final class ConfigConstants {
	private ConfigConstants() {
	}

	/**
	 * The server's login banner text.
	 */
	@TreeConstant
	private static final String server_banner_text = "server.banner.text";

	/**
	 * The server's banner image path.
	 */
	@TreeConstant
	private static final String server_banner_image = "server.banner.image";

	/**
	 * The server's database provider name.
	 */
	@TreeConstant
	private static final String server_db_provider = "server.db.provider";

	/**
	 * The server's database URL.
	 */
	@TreeConstant
	private static final String server_db_url = "server.db.url";

	/**
	 * The server's database username.
	 */
	@TreeConstant
	private static final String server_db_username = "server.db.username";

	/**
	 * The server's database password.
	 */
	@TreeConstant
	private static final String server_db_password = "server.db.password";

	/**
	 * Whether a new debug client will be generated on startup.
	 */
	@TreeConstant
	private static final String server_debug__client = "server.debug_client";

	/**
	 * The server database directory.
	 */
	@TreeConstant
	private static final String server_path_db = "server.path.db";

	/**
	 * The server generator archive directory.
	 */
	@TreeConstant
	private static final String server_path_gen = "server.path.gen";

}
