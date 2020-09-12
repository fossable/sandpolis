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
package com.sandpolis.core.instance.state.oid;

import com.sandpolis.core.instance.state.STAttribute;

/**
 * An {@link OidBase} that corresponds to an {@link STAttribute}.
 *
 * @param <T> The type of the corresponding attribute's value
 */
public class STAttributeOid<T> extends OidBase implements AbsoluteOid<T>, RelativeOid<T> {

	public STAttributeOid(String oid) {
		super(oid);

		if (Oid.type(last()) != Oid.TYPE_ATTRIBUTE)
			throw new IllegalArgumentException();
	}

	public STAttributeOid(int[] oid) {
		super(oid);

		if (Oid.type(last()) != Oid.TYPE_ATTRIBUTE)
			throw new IllegalArgumentException();
	}

	@Override
	public STAttributeOid<?> resolve(int... tags) {
		return resolve(STAttributeOid::new, tags);
	}

	public STAttributeOid<?> resolveLocal() {
		return resolve(9);
	}

	@Override
	public STDocumentOid<?> parent() {
		return parent(STDocumentOid::new);
	}

	@Override
	public STAttributeOid<T> tail() {
		return tail(STAttributeOid::new);
	}
}
