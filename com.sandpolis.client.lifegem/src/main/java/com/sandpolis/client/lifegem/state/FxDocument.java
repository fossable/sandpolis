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
package com.sandpolis.client.lifegem.state;

import java.util.HashMap;

import com.sandpolis.core.instance.state.st.AbstractSTDocument;
import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.st.STObject;
import com.sandpolis.core.instance.state.vst.VirtObject;

public class FxDocument<T extends VirtObject> extends AbstractSTDocument implements STDocument {

	public FxDocument(STObject<?> parent) {
		super(parent, 0);

		documents = new HashMap<>();
		collections = new HashMap<>();
		attributes = new HashMap<>();
	}

}
