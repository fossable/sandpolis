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
package com.sandpolis.core.attribute;

import javax.persistence.Id;

import com.google.protobuf.ByteString;

/**
 * Corresponds to an {@link AttributeDomain}.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class AttributeDomainKey extends AttributeGroupKey {

	/**
	 * The default attribute domain.
	 */
	public static final AttributeDomainKey DEFAULT = new AttributeDomainKey(null);

	/**
	 * A dotted domain or {@code null} for the default domain.
	 */
	@Id
	private String domain;

	public AttributeDomainKey(String domain) {
		this.domain = domain;
		this.key = ByteString.EMPTY;
	}

	/**
	 * Get the domain identifier.
	 * 
	 * @return The domain or {@code null} for the default domain
	 */
	public String getDomain() {
		return domain;
	}
}
