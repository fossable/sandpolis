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

import com.sandpolis.core.instance.State.ProtoCollection;
import com.sandpolis.core.instance.state.st.AbstractSTCollection;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.store.StoreMetadata;

public class EphemeralCollection extends AbstractSTCollection implements STCollection {

	private final Metadata metadata = new Metadata();

	public EphemeralCollection(STDocument parent, long id) {
		super(parent, id);

		documents = new HashMap<>();
	}

	public EphemeralCollection(STDocument parent, long id, ProtoCollection collection) {
		this(parent, id);
		merge(collection);
	}

	@Override
	public StoreMetadata getMetadata() {
		return metadata;
	}

	private class Metadata implements StoreMetadata {

		@Override
		public int getInitCount() {
			return 1;
		}
	}
}
