//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.state;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.AbstractSTAttribute;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STAttributeValue;
import com.sandpolis.core.instance.state.st.STDocument;

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

	public HibernateAttribute(STDocument parent, Oid oid) {
		super(parent, oid);
		this.db_id = UUID.randomUUID().toString();
	}

//	protected HibernateAttribute() {
//		// JPA Constructor
//	}

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

	}
}
