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
package com.sandpolis.core.attribute;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * An {@link AttributeDomain} is a special group that exists only as the root of
 * an attribute tree. AttributeDomains cannot be plural.
 *
 * @author cilki
 * @since 5.0.0
 */
@Entity
public class AttributeDomain extends AttributeGroup {

	/**
	 * A dotted domain or {@code null} for the default domain.
	 */
	@Column
	private String domain;

	public AttributeDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * Get the domain identifier.
	 *
	 * @return The domain or {@code null} for the default domain
	 */
	public String getDomain() {
		return domain;
	}

	AttributeDomain() {
	}
}
