/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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

/**
 * This constant tree contains static keys for permission constants.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class PermissionConstants {
	private PermissionConstants() {
	}

	/**
	 * The super permission which is equivalent to granting all permissions.
	 */
	@TreeConstant
	public static final short omni = 0;

	/**
	 * Permission to generate a MEGA payload.
	 */
	@TreeConstant
	public static final short server_generator_generate__mega = 100;

	/**
	 * Permission to generate a MICRO payload.
	 */
	@TreeConstant
	public static final short server_generator_generate__micro = 101;

	/**
	 * Permission to generate a configuration payload.
	 */
	@TreeConstant
	public static final short server_generator_generate__config = 102;

	/**
	 * Permission to read from the server's filesystem.
	 */
	@TreeConstant
	public static final short server_fs_read = 200;

	/**
	 * Permission to write to the server's filesystem.
	 */
	@TreeConstant
	public static final short server_fs_write = 201;

	/**
	 * Permission to view all listeners.
	 */
	@TreeConstant
	public static final short server_listener_view = 500;

	/**
	 * Permission to create a new listener.
	 */
	@TreeConstant
	public static final short server_listener_create = 501;

	/**
	 * Permission to view all user accounts.
	 */
	@TreeConstant
	public static final short server_user_view = 600;

	/**
	 * Permission to create a new account.
	 */
	@TreeConstant
	public static final short server_user_create = 601;

	/**
	 * Permission to edit any account.
	 */
	@TreeConstant
	public static final short server_user_edit = 602;

	/**
	 * Permission to view all groups.
	 */
	@TreeConstant
	public static final short server_group_view = 700;

	/**
	 * Permission to create a new group.
	 */
	@TreeConstant
	public static final short server_group_create = 701;

	/**
	 * Permission to edit any group.
	 */
	@TreeConstant
	public static final short server_group_edit = 702;

	/**
	 * Permission to see client in list/graph
	 */
	@TreeConstant
	public static final short client_visibility = 800;

	/**
	 * Permission to shutdown the client.
	 */
	@TreeConstant
	public static final short client_power_shutdown = 900;

	/**
	 * Permission to restart the client.
	 */
	@TreeConstant
	public static final short client_power_restart = 901;

	/**
	 * Permission to hibernate the client.
	 */
	@TreeConstant
	public static final short client_power_hibernate = 902;

	/**
	 * Permission to put the client into standby mode.
	 */
	@TreeConstant
	public static final short client_power_standby = 903;

	/**
	 * Permission to put the client into sleep mode.
	 */
	@TreeConstant
	public static final short client_power_sleep = 904;

	/**
	 * Permission to read from the client's filesystem.
	 */
	@TreeConstant
	public static final short client_fs_read = 1000;

	/**
	 * Permission to write to the client's filesystem.
	 */
	@TreeConstant
	public static final short client_fs_write = 1001;

}
