//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.profile.store;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.sandpolis.core.instance.Attribute.ProtoProfile;
import com.sandpolis.core.instance.ProtoType;
import com.sandpolis.core.instance.Result.ErrorCode;
import com.sandpolis.core.instance.storage.database.converter.InstanceConverter;
import com.sandpolis.core.instance.storage.database.converter.InstanceFlavorConverter;
import com.sandpolis.core.profile.AK_INSTANCE;
import com.sandpolis.core.profile.attribute.Attribute;
import com.sandpolis.core.profile.attribute.Document;
import com.sandpolis.core.profile.attribute.key.AttributeKey;
import com.sandpolis.core.util.Platform.Instance;
import com.sandpolis.core.util.Platform.InstanceFlavor;

/**
 * A {@link Profile} is a generic container that stores data for an instance.
 * Most of the data are stored in a tree structure similar to a document store.
 *
 * @author cilki
 * @since 4.0.0
 */
@Entity
public class Profile implements ProtoType<ProtoProfile> {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn
	private Map<String, Document> root;

	/**
	 * The profile's instance type.
	 */
	@Column
	@Convert(converter = InstanceConverter.class)
	private Instance instance;

	/**
	 * The profile's instance subtype.
	 */
	@Column
	@Convert(converter = InstanceFlavorConverter.class)
	private InstanceFlavor flavor;

	/**
	 * The profile's UUID.
	 */
	@Column
	private String uuid;

	@Transient
	private int cvid;

	/**
	 * Initialize an empty {@link Profile}.
	 *
	 * @param uuid     The profile's permanent UUID
	 * @param instance The profile's instance type
	 * @param flavor   The profile's instance subtype
	 */
	public Profile(String uuid, Instance instance, InstanceFlavor flavor) {
		this.uuid = checkNotNull(uuid);
		this.instance = checkNotNull(instance);
		this.flavor = checkNotNull(flavor);

		this.root = new HashMap<>();

		set(AK_INSTANCE.UUID, uuid);
	}

	protected Profile() {
	}

	/**
	 * Get an {@link Attribute}. If the attribute doesn't exist, it will be created.
	 *
	 * @param key The {@link AttributeKey}
	 * @return The corresponding attribute
	 */
	public <E> Attribute<E> getAttribute(AttributeKey<E> key) {
		checkNotNull(key);
		checkArgument(key.isResolved());

		Document document = root.get(key.getDomain());
		if (document == null) {
			document = new Document("");
			root.put(key.getDomain(), document);
		}

		var path = key.getPath().split("/");
		var resolved = key.getResolvedPath().split("/");

		for (int i = 0; i < path.length - 1; i++) {
			if (path[i + 1].equals("_")) {
				// This is a collection
				document = document.collection(path[i++]).document(resolved[i]);
			} else {
				// This is a document
				document = document.document(path[i]);
			}
		}

		// This is the desired attribute
		return document.attribute(key, path[path.length - 1]);
	}

	public boolean hasAttribute(AttributeKey<?> key) {
		Document document = root.get(key.getDomain());
		if (document == null) {
			return false;
		}

		// TODO
		return false;
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
	public ErrorCode merge(ProtoProfile delta) throws Exception {
		// TODO
		return ErrorCode.OK;
	}

	@Override
	public ProtoProfile extract() {
		// TODO
		return null;
	}

	/**
	 * Get the profile's instance.
	 *
	 * @return The profile's instance type
	 */
	public Instance getInstance() {
		return instance;
	}

	public String getUuid() {
		return uuid;
	}

	public int getCvid() {
		return cvid;
	}

	public void setCvid(int cvid) {
		this.cvid = cvid;
		set(AK_INSTANCE.CVID, cvid);
	}

}
