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
package com.sandpolis.core.instance.util;

import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_ADDRESS;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_CERTIFICATE;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_EMAIL;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_GROUPNAME;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_ID;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_KEY;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_PORT;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.proto.util.Result.ErrorCode.OK;
import static com.sandpolis.core.util.ValidationUtil.email;
import static com.sandpolis.core.util.ValidationUtil.group;
import static com.sandpolis.core.util.ValidationUtil.ipv4;
import static com.sandpolis.core.util.ValidationUtil.port;
import static com.sandpolis.core.util.ValidationUtil.username;

import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import com.sandpolis.core.proto.pojo.Group.GroupConfig;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.pojo.User.UserConfig;
import com.sandpolis.core.proto.util.Result.ErrorCode;
import com.sandpolis.core.util.CertUtil;

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
public final class ConfigUtil {

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
		if (config.hasAddress() && !ipv4(config.getAddress()))
			return INVALID_ADDRESS;
		if (!config.hasCert() && config.hasKey())
			return INVALID_CERTIFICATE;
		if (config.hasCert() && !config.hasKey())
			return INVALID_KEY;
		if (config.hasCert() && config.hasKey()) {
			// Check certificate and key formats
			try {
				CertUtil.parseCert(config.getCert().toByteArray());
			} catch (CertificateException e) {
				return INVALID_CERTIFICATE;
			}

			try {
				CertUtil.parseKey(config.getKey().toByteArray());
			} catch (InvalidKeySpecException e) {
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
		if (config.hasEmail() && !email(config.getEmail()))
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
