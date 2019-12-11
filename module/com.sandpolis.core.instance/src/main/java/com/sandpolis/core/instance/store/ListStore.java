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

import org.slf4j.Logger;

import com.sandpolis.core.instance.store.StoreBase.StoreConfig;

public abstract class ListStore<V, E extends StoreConfig> extends StoreBase<E> {

	protected ListStore(Logger log) {
		super(log);
	}

}
