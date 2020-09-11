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

import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;

/**
 * An {@link OidBase} that corresponds to a {@link STDocument}.
 *
 * @param <T> The type of the corresponding document
 */
public class STDocumentOid<T extends VirtObject> extends OidBase implements AbsoluteOid<T>, RelativeOid<T> {

	public STDocumentOid(String oid) {
		super(oid);
	}

	public STDocumentOid(int[] oid) {
		super(oid);
	}

	@Override
	public STDocumentOid<?> resolve(int... tags) {
		return resolve(STDocumentOid::new, tags);
	}

	public STDocumentOid<?> resolveLocal() {
		return resolve(2);
	}

	@Override
	public STDocumentOid<?> head(int length) {
		return head(STDocumentOid::new, length);
	}

	@Override
	public STDocumentOid<?> child(int tag) {
		return child(STDocumentOid::new, tag);
	}

	@Override
	public Oid parent() {
		return parent(AbsoluteOidImpl::new);
	}

	@Override
	public STDocumentOid<T> tail() {
		return tail(STDocumentOid::new);
	}
}
