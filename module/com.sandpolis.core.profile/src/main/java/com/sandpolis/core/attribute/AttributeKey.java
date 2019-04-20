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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.google.protobuf.ByteString;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.OsType;

/**
 * An {@link AttributeKey} corresponds to an {@link Attribute} in an attribute
 * tree.
 * 
 * @author cilki
 * @since 4.0.0
 */
public class AttributeKey<E> extends AttributeNodeKey {

	/**
	 * The type that will be used to build new {@link Attribute}s if a custom
	 * {@link #factory} is not specified.
	 */
	private static Class<?> defaultAttributeType = UntrackedAttribute.class;

	/**
	 * A custom factory for building {@link Attribute}s.
	 */
	private Supplier<Attribute<E>> factory;

	private Set<Instance> instanceWhitelist;
	private Set<OsType> platformWhitelist;
	private Set<Instance> instanceBlacklist;
	private Set<OsType> platformBlacklist;

	private boolean dynamic;
	private String dotPath;

	@SuppressWarnings("rawtypes")
	public static void setDefaultAttributeClass(Class<? extends Attribute> cls) {
		try {
			checkArgument(cls.getConstructor(AttributeKey.class) != null);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		defaultAttributeType = Objects.requireNonNull(cls);
	}

	private AttributeKey(AttributeNodeKey parent, int characteristic) {
		this.parent = Objects.requireNonNull(parent);
		this.key = parent.key.concat(ByteString.copyFrom(new byte[] { (byte) characteristic }));
	}

	private AttributeKey(AttributeNodeKey parent, ByteString key) {
		this.parent = Objects.requireNonNull(parent);
		this.key = Objects.requireNonNull(key);
	}

	/**
	 * Check whether the given instance type is compatible with the corresponding
	 * attribute.
	 * 
	 * @param instance The instance
	 * @return Whether the given instance type is compatible
	 */
	public boolean isCompatible(Instance instance) {
		if (instanceWhitelist != null)
			return instanceWhitelist.contains(instance);
		if (instanceBlacklist != null)
			return !instanceBlacklist.contains(instance);
		return true;
	}

	/**
	 * Check whether the given OS type is compatible with the corresponding
	 * attribute.
	 * 
	 * @param os The OS type
	 * @return Whether the given instance type is compatible
	 */
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

	/**
	 * Get the dot path.
	 * 
	 * @return The key's dot path
	 */
	public String getDotPath() {
		return dotPath;
	}

	/**
	 * Build a new attribute using the attribute factory.
	 * 
	 * @return A new attribute
	 */
	@SuppressWarnings("unchecked")
	public Attribute<E> newAttribute() {
		if (factory != null)
			return factory.get();

		try {
			return (Attribute<E>) defaultAttributeType.getConstructor(AttributeKey.class).newInstance(this);
		} catch (Exception e) {
			// Type was not properly checked when set
			throw new RuntimeException(e);
		}
	}

	/**
	 * Build a new {@link AttributeKey} that is specific to the given group and id.
	 * 
	 * @param group The target group
	 * @param id    An ID within the target group
	 * @return A new {@link AttributeKey} derived from {@code this}
	 */
	public AttributeKey<E> derive(AttributeGroupKey group, ByteString id) {
		checkArgument(isAncestor(group), "The target group key must contain this attribute key");
		checkArgument(group.getPlurality() != 0, "The given target group is not plural");
		checkArgument(group.getPlurality() == id.size(), "The given ID is the wrong size");

		ByteString newStr = group.chain().concat(id);
		newStr = newStr.concat(chain().substring(newStr.size() + 1));

		return new AttributeKey<>(parent, newStr);
	}

	/**
	 * Override the default attribute factory for the {@link Attribute} that
	 * corresponds to this {@link AttributeKey}.
	 * 
	 * @param factory An attribute factory
	 */
	public void setFactory(Supplier<Attribute<E>> factory) {
		this.factory = Objects.requireNonNull(factory);
	}

	public static Builder newBuilder(AttributeNodeKey parent, int id) {
		return new Builder(parent, id);
	}

	/**
	 * A builder for {@link AttributeKey}s.
	 */
	public static class Builder {

		private AttributeKey<?> instance;

		private Builder(AttributeNodeKey parent, int id) {
			instance = new AttributeKey<>(parent, id);
		}

		/**
		 * Add one or more {@link Instance}s to the whitelist.
		 * 
		 * @param compatible Instances that will be whitelisted
		 * @return {@code this}
		 */
		public Builder compatible(Instance... compatible) {
			checkState(instance.instanceWhitelist == null);

			instance.instanceWhitelist = Set.of(compatible);
			return this;
		}

		/**
		 * Add one or more {@link OsType}s to the whitelist.
		 * 
		 * @param compatible Platforms that will be whitelisted
		 * @return {@code this}
		 */
		public Builder compatible(OsType... compatible) {
			checkState(instance.platformWhitelist == null);

			instance.platformWhitelist = Set.of(compatible);
			return this;
		}

		/**
		 * Add one or more {@link Instance}s to the blacklist.
		 * 
		 * @param incompatible Instances that will be blacklisted
		 * @return {@code this}
		 */
		public Builder incompatible(Instance... incompatible) {
			checkState(instance.instanceBlacklist == null);

			instance.instanceBlacklist = Set.of(incompatible);
			return this;
		}

		/**
		 * Add one or more {@link OsType}s to the blacklist.
		 * 
		 * @param incompatible Platforms that will be blacklisted
		 * @return {@code this}
		 */
		public Builder incompatible(OsType... incompatible) {
			checkState(instance.platformBlacklist == null);

			instance.platformBlacklist = Set.of(incompatible);
			return this;
		}

		/**
		 * Set the {@code static} flag.
		 * 
		 * @param value The flag's new value
		 * @return {@code this}
		 */
		public Builder setStatic(boolean value) {
			instance.dynamic = !value;
			return this;
		}

		/**
		 * Set the {@code dotPath} field.
		 * 
		 * @param value The field's new value
		 * @return {@code this}
		 */
		public Builder setDotPath(String value) {
			instance.dotPath = value;
			return this;
		}

		/**
		 * Build the {@link AttributeKey} from the current state of this builder.
		 * 
		 * @return A new attribute key
		 */
		@SuppressWarnings("unchecked")
		public <E> AttributeKey<E> build() {
			return (AttributeKey<E>) instance;
		}
	}
}
