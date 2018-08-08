/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.OsType;

/**
 * An {@link AttributeKey} uniquely identifies a specific attribute within an
 * {@link AttributeGroup}.<br>
 * <br>
 * 
 * All attribute keys conform to a naming standard (AK_*) for identifiability.
 * 
 * @author cilki
 * @since 4.0.0
 */
public class AttributeKey<E> extends AttributeNodeKey {

	private Set<Instance> instanceWhitelist;
	private Set<OsType> platformWhitelist;
	private Set<Instance> instanceBlacklist;
	private Set<OsType> platformBlacklist;

	private boolean dynamic;

	private AttributeKey(AttributeNodeKey parent, int id, Set<Instance> instanceWhitelist,
			Set<OsType> platformWhitelist, Set<Instance> instanceBlacklist, Set<OsType> platformBlacklist,
			boolean dynamic) {
		super(parent, id);

		this.instanceWhitelist = instanceWhitelist;
		this.platformWhitelist = platformWhitelist;
		this.instanceBlacklist = instanceBlacklist;
		this.platformBlacklist = platformBlacklist;

		this.dynamic = dynamic;
	}

	public boolean isCompatible(Instance instance) {
		if (instanceWhitelist != null)
			return instanceWhitelist.contains(instance);
		if (instanceBlacklist != null)
			return !instanceBlacklist.contains(instance);
		return true;
	}

	public boolean isCompatible(OsType os) {
		if (platformWhitelist != null)
			return platformWhitelist.contains(os);
		if (platformBlacklist != null)
			return !platformBlacklist.contains(os);
		return true;
	}

	/**
	 * Indicates whether the associated attribute is static or dynamic. Static
	 * attributes are not expected to change throughout the lifetime of an attribute
	 * group.
	 * 
	 * @return Whether the associated attribute is static
	 */
	public boolean isStatic() {
		return !dynamic;
	}

	public static Builder newBuilder(AttributeNodeKey parent, int id) {
		return new Builder(parent, id);
	}

	/**
	 * A builder for {@link AttributeKey}s.
	 */
	public static class Builder {

		private AttributeNodeKey parent;
		private int id;

		private Set<Instance> instanceWhitelist;
		private Set<OsType> platformWhitelist;
		private Set<Instance> instanceBlacklist;
		private Set<OsType> platformBlacklist;

		private boolean dynamic;

		private Builder(AttributeNodeKey parent, int id) {
			this.parent = parent;
			this.id = id;
		}

		/**
		 * Add one or more {@link Instance}s to the whitelist.
		 * 
		 * @param compatible Instances that will be whitelisted
		 * @return {@code this}
		 */
		public Builder compatible(Instance... compatible) {
			if (instanceWhitelist == null)
				instanceWhitelist = new HashSet<>();
			instanceWhitelist.addAll(Arrays.asList(compatible));
			return this;
		}

		/**
		 * Add one or more {@link OsType}s to the whitelist.
		 * 
		 * @param compatible Platforms that will be whitelisted
		 * @return {@code this}
		 */
		public Builder compatible(OsType... compatible) {
			if (platformWhitelist == null)
				platformWhitelist = new HashSet<>();
			platformWhitelist.addAll(Arrays.asList(compatible));
			return this;
		}

		/**
		 * Add one or more {@link Instance}s to the blacklist.
		 * 
		 * @param incompatible Instances that will be blacklisted
		 * @return {@code this}
		 */
		public Builder incompatible(Instance... incompatible) {
			if (instanceBlacklist == null)
				instanceBlacklist = new HashSet<>();
			instanceBlacklist.addAll(Arrays.asList(incompatible));
			return this;
		}

		/**
		 * Add one or more {@link OsType}s to the blacklist.
		 * 
		 * @param incompatible Platforms that will be blacklisted
		 * @return {@code this}
		 */
		public Builder incompatible(OsType... incompatible) {
			if (platformBlacklist == null)
				platformBlacklist = new HashSet<>();
			platformBlacklist.addAll(Arrays.asList(incompatible));
			return this;
		}

		/**
		 * Set the {@code static} flag.
		 * 
		 * @param value The flag's new value
		 * @return {@code this}
		 */
		public Builder setStatic(boolean value) {
			dynamic = !value;
			return this;
		}

		/**
		 * Build the {@link AttributeKey} from the current state of this builder.
		 * 
		 * @return A new attribute key
		 */
		public <E> AttributeKey<E> build() {
			return new AttributeKey<>(parent, id, instanceWhitelist, platformWhitelist, instanceBlacklist,
					platformBlacklist, dynamic);
		}
	}
}
