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

import java.util.HashMap;

import com.sandpolis.core.instance.State.ProtoDocument;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.st.AbstractSTDocument;
import com.sandpolis.core.instance.state.st.STDocument;

/**
 * {@link EphemeralDocument} is a memory-only implementation of
 * {@link STDocument}.
 *
 * @since 5.1.1
 */
public class EphemeralDocument extends AbstractSTDocument implements STDocument {

	public EphemeralDocument(STDocument parent, Oid oid) {
		super(parent, oid);

		documents = new HashMap<>();
		attributes = new HashMap<>();
	}

	public EphemeralDocument(STDocument parent, Oid oid, ProtoDocument document) {
		this(parent, oid);
		merge(document);
	}

	public EphemeralDocument() {
		this(null, AbsoluteOid.ROOT);
	}

	public EphemeralDocument(ProtoDocument document) {
		this();
		merge(document);
	}
}
