//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance.state.vst;

import com.sandpolis.core.instance.state.oid.Oid;
import com.sandpolis.core.instance.state.oid.RelativeOid;
import com.sandpolis.core.instance.state.st.STAttribute;
import com.sandpolis.core.instance.state.st.STDocument;

public abstract class VirtDocument implements VirtObject {

	protected STDocument document;

	public VirtDocument(STDocument document) {
		this.document = document;
	}

	public Oid oid() {
		return document.oid();
	}

	public abstract String getId();

	public abstract STAttribute<String> id();

	public <T> STAttribute<T> attribute(RelativeOid<STAttribute<T>> oid) {
		return document.getAttribute(oid);
	}

	public <T> T get(RelativeOid<STAttribute<T>> oid) {
		return attribute(oid).get();
	}

	public <T> void set(RelativeOid<STAttribute<T>> oid, T value) {
		attribute(oid).set(value);
	}
}
