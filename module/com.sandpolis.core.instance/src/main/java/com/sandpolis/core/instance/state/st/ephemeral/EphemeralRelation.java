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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sandpolis.core.instance.state.st.STDocument;
import com.sandpolis.core.instance.state.st.STRelation;
import com.sandpolis.core.instance.state.vst.VirtObject;

public class EphemeralRelation<T extends VirtObject> implements STRelation<T> {

	private List<T> container;

	public EphemeralRelation(Function<STDocument, T> constructor) {
		container = new ArrayList<>();
	}

	@Override
	public void add(T element) {
		container.add(element);
	}

	@Override
	public Stream<T> stream() {
		return container.stream();
	}

	@Override
	public int size() {
		return container.size();
	}

	@Override
	public boolean contains(T element) {
		return container.contains(element);
	}

}
