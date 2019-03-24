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

/**
 * This constant tree contains static keys for permission constants.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class Perm {
	private Perm() {
	}

	/**
	 * The super permission which is equivalent to granting all permissions.
	 */
	public static final short omni = 0;

	public static final class server {
		private server() {
		}

		public static final class generator {
			private generator() {
			}

			/**
			 * Permission to generate a MEGA payload.
			 */
			public static final short generate_mega = 100;

			/**
			 * Permission to generate a MICRO payload.
			 */
			public static final short generate_micro = 101;

			/**
			 * Permission to generate a configuration payload.
			 */
			public static final short generate_config = 102;
		}

		public static final class fs {
			private fs() {
			}

			/**
			 * Permission to read from the server's filesystem.
			 */
			public static final short read = 200;

			/**
			 * Permission to write to the server's filesystem.
			 */
			public static final short write = 201;
		}

		public static final class listeners {
			private listeners() {
			}

			/**
			 * Permission to view all listeners.
			 */
			public static final short view = 500;

			/**
			 * Permission to create a new listener.
			 */
			public static final short create = 501;
		}

		public static final class users {
			private users() {
			}

			/**
			 * Permission to view all user accounts.
			 */
			public static final short view = 600;

			/**
			 * Permission to create a new account.
			 */
			public static final short create = 601;

			/**
			 * Permission to edit any account.
			 */
			public static final short edit = 602;
		}

		public static final class groups {
			private groups() {
			}

			/**
			 * Permission to view all groups.
			 */
			public static final short view = 700;

			/**
			 * Permission to create a new group.
			 */
			public static final short create = 701;

			/**
			 * Permission to edit any group.
			 */
			public static final short edit = 702;
		}
	}

	public static final class client {

		/**
		 * Permission to see client in list/graph
		 */
		public static final short visibility = 800;

		public static final class power {
			private power() {
			}

			/**
			 * Permission to shutdown the client.
			 */
			public static final short shutdown = 900;

			/**
			 * Permission to restart the client.
			 */
			public static final short restart = 901;

			/**
			 * Permission to hibernate the client.
			 */
			public static final short hibernate = 902;

			/**
			 * Permission to put the client into standby mode.
			 */
			public static final short standby = 903;

			/**
			 * Permission to put the client into sleep mode.
			 */
			public static final short sleep = 904;
		}

		public static final class fs {
			private fs() {
			}

			/**
			 * Permission to read from the client's filesystem.
			 */
			public static final short read = 1000;

			/**
			 * Permission to write to the client's filesystem.
			 */
			public static final short write = 1001;
		}

		public static final class shell {
			private shell() {
			}

			/**
			 * Permission to open a shell session.
			 */
			public static final short execute = 1100;
		}

		public static final class keylogger {

			/**
			 * Permission to change keylogger settings.
			 */
			public static final short edit = 1200;

			/**
			 * Permission to read keylogs.
			 */
			public static final short read = 1201;
		}
	}
}
