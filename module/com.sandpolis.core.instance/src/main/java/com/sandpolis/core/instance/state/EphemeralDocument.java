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
package com.sandpolis.core.instance.state;

import java.util.HashMap;

import com.sandpolis.core.instance.State.ProtoDocument;

/**
 * {@link EphemeralDocument} is a memory-only implementation of
 * {@link STDocument}.
 *
 * @since 5.1.1
 */
public class EphemeralDocument extends AbstractSTDocument implements STDocument {

	public EphemeralDocument(STDocument parent) {
		this.parent = (AbstractSTObject) parent;

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public EphemeralDocument(STDocument parent, ProtoDocument document) {
		this(parent);
		merge(document);
	}

	public EphemeralDocument(STCollection parent) {
		this.parent = (AbstractSTObject) parent;

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

	public EphemeralDocument(STCollection parent, ProtoDocument document) {
		this(parent);
		merge(document);
	}

	@Override
	public STAttribute<?> newAttribute() {
		return new EphemeralAttribute<>(this);
	}

	@Override
	public STDocument newDocument() {
		return new EphemeralDocument(this);
	}

	@Override
	public STCollection newCollection() {
		return new EphemeralCollection(this);
	}

}
