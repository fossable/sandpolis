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
import com.sandpolis.core.instance.state.st.AbstractSTDocument;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.instance.state.st.STDocument;

/**
 * {@link EphemeralDocument} is a memory-only implementation of
 * {@link STDocument}.
 *
 * @since 5.1.1
 */
public class EphemeralDocument extends AbstractSTDocument implements STDocument {

	public EphemeralDocument(STDocument parent, long id) {
		super(parent, id);

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public EphemeralDocument(STDocument parent, long id, ProtoDocument document) {
		this(parent, id);
		merge(document);
	}

	public EphemeralDocument(STCollection parent, long id) {
		super(parent, id);

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public EphemeralDocument(STCollection parent, long id, ProtoDocument document) {
		this(parent, id);
		merge(document);
	}
}
