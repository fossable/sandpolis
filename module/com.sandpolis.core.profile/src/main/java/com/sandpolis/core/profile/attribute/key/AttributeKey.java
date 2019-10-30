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
package com.sandpolis.core.profile.attribute.key;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sandpolis.core.profile.attribute.Attribute;
import com.sandpolis.core.profile.attribute.Attribute.IntegerAttribute;
import com.sandpolis.core.profile.attribute.Attribute.LongAttribute;
import com.sandpolis.core.profile.attribute.Attribute.OsTypeAttribute;
import com.sandpolis.core.profile.attribute.Attribute.StringAttribute;
import com.sandpolis.core.proto.util.Platform.OsType;

/**
 * An {@link AttributeKey} corresponds to an {@link Attribute} in an attribute
 * tree.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class AttributeKey<E> {

	/**
	 * A map of auxiliary objects related to this key.
	 */
	private final Map<String, Object> aux = new HashMap<>();

	private final String path;

	private final String resolved;

	private final Class<E> type;

	private final String domain;

	private Supplier<E> query;

	public AttributeKey(String domain, Class<E> type, String path) {
		this(domain, type, path, path);
	}

	private AttributeKey(String domain, Class<E> type, String path, String resolved) {
		this.domain = domain;
		this.type = type;
		this.path = path;
		this.resolved = resolved;
	}

	/**
	 * Build a new attribute using the attribute factory.
	 *
	 * @return A new attribute
	 */
	@SuppressWarnings("unchecked")
	public Attribute<E> newAttribute() {
		if (type == String.class) {
			return (Attribute<E>) new StringAttribute();
		} else if (type == Integer.class) {
			return (Attribute<E>) new IntegerAttribute();
		} else if (type == Long.class) {
			return (Attribute<E>) new LongAttribute();
		} else if (type == OsType.class) {
			return (Attribute<E>) new OsTypeAttribute();
		}

		return null;
	}

	public AttributeKey<E> derive(String... id) {
		var res = resolved;
		for (String s : id) {
			res = res.replaceFirst("_", s);
		}

		return new AttributeKey<>(domain, type, path, res);
	}

	/**
	 * Get whether the given id has an associated auxiliary object for {@code this}.
	 *
	 * @param id The auxiliary object id
	 * @return Whether {@code this} has an object associated with id
	 */
	public boolean containsObject(String id) {
		return aux.containsKey(id);
	}

	/**
	 * Get the auxiliary object associated with the given id.
	 *
	 * @param id The auxiliary object id
	 * @return The requested auxiliary object
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObject(String id) {
		return (T) aux.get(id);
	}

	/**
	 * Associate the given auxiliary object with the given id.
	 *
	 * @param id    The auxiliary object id
	 * @param value The new object
	 */
	public void putObject(String id, Object value) {
		aux.put(id, value);
	}

	public void setQuery(Supplier<E> query) {
		this.query = query;
	}

	public void setQuery(Function<?, E> query) {
	}

	public String getDomain() {
		return domain;
	}

	public String getPath() {
		return path;
	}

	public String getResolvedPath() {
		return resolved;
	}

	public boolean isResolved() {
		// Just check for placeholders
		return !resolved.contains("/_/");
	}
}
