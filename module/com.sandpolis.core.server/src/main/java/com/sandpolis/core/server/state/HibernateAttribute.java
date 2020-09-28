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
package com.sandpolis.core.server.state;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.sandpolis.core.instance.State.ProtoAttribute;
import com.sandpolis.core.instance.state.AbstractSTAttribute;
import com.sandpolis.core.instance.state.AbstractSTObject;
import com.sandpolis.core.instance.state.STAttribute;
import com.sandpolis.core.instance.state.STAttributeValue;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;

/**
 * {@link HibernateAttribute} allows attributes to be persistent and optionally
 * saves the history of the attribute's value.
 *
 * @param <T> The type of the attribute's value
 * @since 7.0.0
 */
@Entity
public class HibernateAttribute<T> extends AbstractSTAttribute<T> implements STAttribute<T> {

	@Id
	private String db_id;

	public HibernateAttribute(HibernateDocument parent) {
		this.parent = parent;
		this.db_id = UUID.randomUUID().toString();
	}

	protected HibernateAttribute() {
		// JPA Constructor
	}

	@Column
	@Override
	public long getTag() {
		return tag;
	}

	@Override
	public Oid oid() {
		return parent.oid().child(tag);
	}

	@Override
	public void setTag(long tag) {
		this.tag = tag;
	}

	@Embedded
	protected HibernateAttributeValue<T> getCurrent() {
		return (HibernateAttributeValue<T>) current;
	}

	@ElementCollection
	protected List<HibernateAttributeValue<T>> getHistory() {
		return (List) history;
	}

	@Column(nullable = true)
	protected HibernateDocument getParent() {
		return (HibernateDocument) parent;
	}

	@Column(nullable = true)
	protected RetentionPolicy getRetention() {
		return retention;
	}

	@Column(nullable = true)
	protected long getRetentionLimit() {
		return retentionLimit;
	}

	@Override
	protected STAttributeValue<T> newValue(T value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected STAttributeValue<T> newValue(T value, long timestamp) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void setParent(HibernateDocument parent) {
		this.parent = parent;
	}
}
