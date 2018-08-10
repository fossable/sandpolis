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

import java.io.File;
import java.security.cert.CertificateException;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.RegexValidator;

import com.google.protobuf.ByteString;
import com.sandpolis.core.proto.util.Listener.ListenerConfig;

/**
 * Utilities to validate user input.
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
			String.format("^[.]{%d,%d}$", GROUP_MIN, GROUP_MAX));

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
	 * Validate a listener configuration.
	 * 
	 * @param config The candidate configuration
	 * @return The configuration's validity
	 */
	public static boolean listenerConfig(ListenerConfig config) {
		if (config == null)
			return false;
		if (!username(config.getOwner()))
			return false;
		if (!port(config.getPort()))
			return false;
		if (!InetAddressValidator.getInstance().isValidInet4Address(config.getAddress()))
			return false;
		if (config.getCert() != ByteString.EMPTY && config.getKey() != ByteString.EMPTY) {
			// Validate certificate and key formats
			try {
				CertUtil.parse(config.getCert().toByteArray());
				CertUtil.parse(config.getKey().toByteArray());
			} catch (CertificateException e) {
				return false;
			}
		}

		return true;
	}

}