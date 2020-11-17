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

import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.st.STObject;

public class AbsoluteOid<T extends STObject<?>> extends RelativeOid<T> {

	public static final AbsoluteOid<STDocument> ROOT = new AbsoluteOid<>();

	public AbsoluteOid(long namespace, String path) {
		super(namespace, path);
	}

	public AbsoluteOid(long namespace, String[] path) {
		super(namespace, path);
	}

	private AbsoluteOid() {
		super(0, "");
	}

}
