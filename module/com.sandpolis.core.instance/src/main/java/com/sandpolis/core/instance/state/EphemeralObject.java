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

import com.sandpolis.core.instance.state.oid.AbsoluteOid.AbsoluteOidImpl;
import com.sandpolis.core.instance.state.oid.Oid;

public abstract class EphemeralObject extends AbstractSTObject {

	private int tag;

	public Oid oid() {
		if (parent() == null) {
			return new AbsoluteOidImpl<>(tag);
		}

		var parentOid = ((EphemeralObject) parent()).oid();
		if (parentOid == null) {
			return new AbsoluteOidImpl<>(tag);
		}

		return parentOid.child(tag);
	}

	@Override
	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}
}
