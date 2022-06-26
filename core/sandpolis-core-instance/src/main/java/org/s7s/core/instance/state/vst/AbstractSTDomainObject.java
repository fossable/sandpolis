//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.state.vst;

import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.STAttribute;
import org.s7s.core.instance.state.st.STDocument;

public abstract class AbstractSTDomainObject implements STDomainObject {

	protected STDocument document;

	public AbstractSTDomainObject(STDocument document) {
		this.document = document;
	}

	public Oid oid() {
		return document.oid();
	}

	public String getId() {
		return oid().last();
	}

	public STAttribute get(Oid oid) {
		return document.attribute(oid);
	}

	public void set(Oid oid, Object value) {
		get(oid).set(value);
	}
}
