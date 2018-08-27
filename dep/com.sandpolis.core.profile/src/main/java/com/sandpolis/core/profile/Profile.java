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
package com.sandpolis.core.profile;

import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.sandpolis.core.attribute.Attribute;
import com.sandpolis.core.attribute.AttributeGroup;
import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.attribute.AttributeNodeKey;
import com.sandpolis.core.attribute.key.AK_META;
import com.sandpolis.core.instance.Updatable.AbstractUpdatable;
import com.sandpolis.core.instance.storage.database.converter.InstanceConverter;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Update.AttributeNodeUpdate;
import com.sandpolis.core.proto.util.Update.ProfileUpdate;

/**
 * A {@link Profile} is a generic container that stores data for an instance.
 * Every {@link Instance#SERVER}, {@link Instance#CLIENT}, and
 * {@link Instance#VIEWER} may have a separate profile.
 * 
 * @author cilki
 * @since 4.0.0
 */
@Entity
public class Profile extends AbstractUpdatable<ProfileUpdate> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The root node of the attribute tree.
	 */
	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn
	private AttributeGroup root;

	/**
	 * The profile's instance type.
	 */
	@Column
	@Convert(converter = InstanceConverter.class)
	private Instance instance;

	/**
	 * Initialize a {@link Profile}.
	 */
	public Profile(Instance instance) {
		this.instance = Objects.requireNonNull(instance);
		this.root = new AttributeGroup(AttributeNodeKey.ROOT);
	}

	/**
	 * Get the value of the specified {@link Attribute}.
	 * 
	 * @param key The {@link AttributeKey} to query.
	 * @return The value of the corresponding attribute.
	 */
	public <E> E get(AttributeKey<E> key) {
		return getAttribute(key).get();
	}

	/**
	 * Get an {@link Attribute}.
	 * 
	 * @param key The {@link AttributeKey} to query.
	 * @return The corresponding attribute.
	 */
	@SuppressWarnings("unchecked")
	public <E> Attribute<E> getAttribute(AttributeKey<E> key) {
		return (Attribute<E>) root.getNode(key.chain().iterator());
	}

	/**
	 * Set an {@link Attribute}'s value.
	 * 
	 * @param key   The {@link AttributeKey}.
	 * @param value The new value.
	 */
	public <E> void set(AttributeKey<E> key, E value) {
		getAttribute(key).set(value);
	}

	@Override
	public void merge(ProfileUpdate updates) throws Exception {
		if (updates.hasRootUpdate())
			root.merge(updates.getRootUpdate());
	}

	@Override
	public ProfileUpdate getUpdates(long time) {
		AttributeNodeUpdate rootUpdate = root.getUpdates(time);
		if (rootUpdate == null)
			return null;
		return ProfileUpdate.newBuilder().setRootUpdate(rootUpdate).build();
	}

	/**
	 * Get the profile's instance.
	 * 
	 * @return The profile's instance type
	 */
	public Instance getInstance() {
		return instance;
	}

	/**
	 * Shortcut for {@code get(AK_META.UUID);}.
	 * 
	 * @return The current UUID.
	 */
	public String getUuid() {
		return get(AK_META.UUID);
	}

	/**
	 * Shortcut for {@code get(AK_META.CVID);}.
	 * 
	 * @return The current CVID.
	 */
	public int getCvid() {
		return get(AK_META.CVID);
	}

	/**
	 * Shortcut for {@code set(AK_META.CVID, cvid);}.
	 * 
	 * @param cvid The new CVID
	 */
	public void setCvid(int cvid) {
		set(AK_META.CVID, cvid);
	}

}
