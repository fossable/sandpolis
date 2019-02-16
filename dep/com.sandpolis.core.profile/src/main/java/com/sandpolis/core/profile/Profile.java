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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.sandpolis.core.attribute.Attribute;
import com.sandpolis.core.attribute.AttributeDomain;
import com.sandpolis.core.attribute.AttributeKey;
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

	// TODO
	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn
	private AttributeDomain root;

	// TODO
	@OneToMany(cascade = CascadeType.ALL)
	@MapKeyColumn(name = "db_id")
	private Map<String, AttributeDomain> domains;

	/**
	 * The profile's instance type.
	 */
	@Column
	@Convert(converter = InstanceConverter.class)
	private Instance instance;

	@Column
	private String uuid;

	@Column
	private int cvid;

	/**
	 * Initialize a {@link Profile}.
	 */
	public Profile(Instance instance) {
		this.instance = Objects.requireNonNull(instance);
		this.domains = new HashMap<>();
	}

	Profile() {
		// JPA
	}

	/**
	 * Get an {@link Attribute}.
	 * 
	 * @param key The {@link AttributeKey} to query.
	 * @return The corresponding attribute.
	 */
	@SuppressWarnings("unchecked")
	public <E> Attribute<E> getAttribute(AttributeKey<E> key) {
		String domain = key.getDomain();
		if (!domains.containsKey(domain))
			domains.put(domain, new AttributeDomain(domain));

		var attribute = domains.get(domain).getNode(key.chain().iterator());
		if (attribute == null)
			domains.get(domain).addNode(key.newAttribute());

		return (Attribute<E>) domains.get(domain).getNode(key.chain().iterator());
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
		return uuid;
	}

	/**
	 * Shortcut for {@code get(AK_META.CVID);}.
	 * 
	 * @return The current CVID.
	 */
	public int getCvid() {
		return cvid;
	}

	/**
	 * Shortcut for {@code set(AK_META.CVID, cvid);}.
	 * 
	 * @param cvid The new CVID
	 */
	public void setCvid(int cvid) {
		this.cvid = cvid;
	}

}
