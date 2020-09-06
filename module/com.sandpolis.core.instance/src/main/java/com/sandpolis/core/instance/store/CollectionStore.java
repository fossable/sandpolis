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
package com.sandpolis.core.instance.store;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;

public abstract class CollectionStore<V> extends StoreBase {

	protected CollectionStore(Logger log) {
		super(log);
	}

	protected List<V> container;

	public void removeValue(V value) {
		container.remove(value);
	}

	public Stream<V> stream() {
		return container.stream();
	}

	public int size() {
		return container.size();
	}

}
