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
package com.sandpolis.core.instance.state.st.ephemeral;

import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.AbstractSTAttribute;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STAttributeValue;
import com.sandpolis.core.instance.state.st.STDocument;

/**
 * {@link EphemeralAttribute} allows attributes to be persistent and optionally
 * saves the history of the attribute's value.
 *
 * @param <T> The type of the attribute's value
 * @since 7.0.0
 */
public class EphemeralAttribute<T> extends AbstractSTAttribute<T> implements STAttribute<T> {

	public EphemeralAttribute(STDocument parent, Oid oid) {
		super(parent, oid);
	}

	public EphemeralAttribute() {
		this(null, AbsoluteOid.ROOT);
	}

	@Override
	protected STAttributeValue<T> newValue(T value) {
		return new EphemeralAttributeValue<>(value);
	}

	@Override
	protected STAttributeValue<T> newValue(T value, long timestamp) {
		return new EphemeralAttributeValue<>(value, timestamp);
	}
}
