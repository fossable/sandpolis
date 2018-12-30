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
package com.sandpolis.core.util;

import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_ADDRESS;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_CERTIFICATE;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_EMAIL;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_GROUPNAME;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_ID;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_KEY;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_PORT;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.proto.util.Result.ErrorCode.OK;

import java.io.File;
import java.security.cert.CertificateException;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.RegexValidator;

import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.ErrorCode;

/**
 * Utilities that validate user input.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class ValidationUtil {
	private ValidationUtil() {
	}

	/**
	 * The longest possible username.
	 */
	public static final int USER_MAX = 30;

	/**
	 * The shortest possible username.
	 */
	public static final int USER_MIN = 5;

	/**
	 * The longest possible group name.
	 */
	public static final int GROUP_MAX = 48;

	/**
	 * The shortest possible group name.
	 */
	public static final int GROUP_MIN = 4;

	/**
	 * The longest possible password.
	 */
	public static final int PASSWORD_MAX = 64;

	/**
	 * The shortest possible password.
	 */
	public static final int PASSWORD_MIN = 5;

	/**
	 * Username validator.
	 */
	private static final RegexValidator USERNAME_REGEX = new RegexValidator(
			String.format("^[a-zA-Z0-9]{%d,%d}$", USER_MIN, USER_MAX));

	/**
	 * Group name validator.
	 */
	private static final RegexValidator GROUPNAME_REGEX = new RegexValidator(
			String.format("^[a-zA-Z0-9 ]{%d,%d}$", GROUP_MIN, GROUP_MAX));

	/**
	 * Password validator.
	 */
	private static final RegexValidator PASSWORD_REGEX = new RegexValidator(
			String.format("^.{%d,%d}$", PASSWORD_MIN, PASSWORD_MAX));

	/**
	 * Private IPv4 validator.
	 */
	private static final RegexValidator PRIVATEIP_REGEX = new RegexValidator(
			"(^127\\..*$)|(^10\\..*$)|(^172\\.1[6-9]\\..*$)|(^172\\.2[0-9]\\..*$)|(^172\\.3[0-1]\\..*$)|(^192\\.168\\..*$)");

	/**
	 * Version validator.
	 */
	private static final RegexValidator VERSION_REGEX = new RegexValidator("^(\\d)+\\.(\\d)+\\.(\\d)+(-(\\d)+)?$");

	/**
	 * Validate a user name.
	 * 
	 * @param user The candidate username
	 * @return The username validity
	 */
	public static boolean username(String user) {
		return USERNAME_REGEX.isValid(user);
	}

	/**
	 * Validate a group name.
	 * 
	 * @param group The candidate group name
	 * @return The group name validity
	 */
	public static boolean group(String group) {
		return GROUPNAME_REGEX.isValid(group);
	}

	/**
	 * Validate a password.
	 * 
	 * @param password The candidate password
	 * @return The password validity
	 */
	public static boolean password(String password) {
		return PASSWORD_REGEX.isValid(password);
	}

	/**
	 * Validate a private IPv4 address.
	 * 
	 * @param ip The candidate IP address
	 * @return The IP validity
	 */
	public static boolean privateIP(String ip) {
		return InetAddressValidator.getInstance().isValidInet4Address(ip) && PRIVATEIP_REGEX.isValid(ip);
	}

	/**
	 * Validate a port number.
	 * 
	 * @param port The candidate port
	 * @return The port validity
	 */
	public static boolean port(String port) {
		try {
			return port(Integer.parseInt(port));
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Validate a port number.
	 * 
	 * @param port The candidate port
	 * @return The port validity
	 */
	public static boolean port(int port) {
		return (port > 0 && port < 65536);
	}

	/**
	 * Validate a filesystem path.
	 * 
	 * @param path The candidate path
	 * @return The path validity
	 */
	public static boolean path(String path) {
		try {
			new File(path).getCanonicalPath();
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * Validate a Sandpolis version number.
	 * 
	 * @param version The candidate version
	 * @return The version validity
	 */
	public static boolean version(String version) {
		return VERSION_REGEX.isValid(version);
	}

	/**
	 * This static class checks protobuf configurations for validity and
	 * completeness.
	 * 
	 * A config is <b>complete</b> if:
	 * <ul>
	 * <li>Every required field is present in the config.</li>
	 * </ul>
	 * 
	 * A config is <b>valid</b> if:
	 * <ul>
	 * <li>Every field present in the config passes all input restrictions.</li>
	 * </ul>
	 * 
	 * A config may be in any combination of the two states.
	 * 
	 * @author cilki
	 * @since 5.0.0
	 */
	public static class Config {

		/**
		 * Validate a {@link ListenerConfig}.
		 * 
		 * @param config The candidate configuration
		 * @return An error code or {@link ErrorCode#OK}
		 */
		public static ErrorCode valid(ListenerConfig config) {
			if (config == null)
				throw new IllegalArgumentException();

			if (config.hasOwner() && !username(config.getOwner()))
				return INVALID_USERNAME;
			if (config.hasPort() && !port(config.getPort()))
				return INVALID_PORT;
			if (config.hasAddress() && !InetAddressValidator.getInstance().isValidInet4Address(config.getAddress()))
				return INVALID_ADDRESS;
			if (!config.hasCert() && config.hasKey())
				return INVALID_CERTIFICATE;
			if (config.hasCert() && !config.hasKey())
				return INVALID_KEY;
			if (config.hasCert() && config.hasKey()) {
				// Check certificate and key formats
				try {
					CertUtil.parse(config.getCert().toByteArray());
				} catch (CertificateException e) {
					return INVALID_CERTIFICATE;
				}

				try {
					CertUtil.parse(config.getKey().toByteArray());
				} catch (CertificateException e) {
					return INVALID_KEY;
				}
			}

			return OK;
		}

		/**
		 * Check a {@link ListenerConfig} for completeness.
		 * 
		 * @param config The candidate configuration
		 * @return An error code or {@link ErrorCode#OK}
		 */
		public static ErrorCode complete(ListenerConfig config) {
			if (config == null)
				throw new IllegalArgumentException();

			if (!config.hasPort())
				return INVALID_PORT;
			if (!config.hasAddress())
				return INVALID_ADDRESS;
			if (!config.hasOwner())
				return INVALID_USERNAME;

			return OK;
		}

		/**
		 * Validate a {@link UserConfig}.
		 * 
		 * @param config The candidate configuration
		 * @return An error code or {@link ErrorCode#OK}
		 */
		public static ErrorCode valid(UserConfig config) {
			if (config == null)
				throw new IllegalArgumentException();

			if (config.hasUsername() && !username(config.getUsername()))
				return INVALID_USERNAME;
			if (config.hasEmail() && !EmailValidator.getInstance().isValid(config.getEmail()))
				return INVALID_EMAIL;

			return OK;
		}

		/**
		 * Check a {@link UserConfig} for completeness.
		 * 
		 * @param config The candidate configuration
		 * @return An error code or {@link ErrorCode#OK}
		 */
		public static ErrorCode complete(UserConfig config) {
			if (config == null)
				throw new IllegalArgumentException();

			if (!config.hasUsername())
				return INVALID_USERNAME;

			return OK;
		}

		/**
		 * Validate a {@link GroupConfig}.
		 * 
		 * @param config The candidate configuration
		 * @return An error code or {@link ErrorCode#OK}
		 */
		public static ErrorCode valid(GroupConfig config) {
			if (config == null)
				throw new IllegalArgumentException();

			if (config.hasName() && !group(config.getName()))
				return INVALID_GROUPNAME;
			if (config.hasOwner() && !username(config.getOwner()))
				return INVALID_USERNAME;
			for (String member : config.getMemberList())
				if (!username(member))
					return INVALID_USERNAME;

			return OK;
		}

		/**
		 * Check a {@link GroupConfig} for completeness.
		 * 
		 * @param config The candidate configuration
		 * @return An error code or {@link ErrorCode#OK}
		 */
		public static ErrorCode complete(GroupConfig config) {
			if (config == null)
				throw new IllegalArgumentException();

			if (!config.hasId())
				return INVALID_ID;
			if (!config.hasOwner())
				return INVALID_USERNAME;

			return OK;
		}
	}
}