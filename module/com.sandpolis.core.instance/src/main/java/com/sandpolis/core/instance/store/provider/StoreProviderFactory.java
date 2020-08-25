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
package com.sandpolis.core.instance.store.provider;

import java.util.function.Function;

import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.STDocument;
import com.sandpolis.core.instance.state.VirtObject;

public interface StoreProviderFactory {

	public <E extends VirtObject> StoreProvider<E> supply(Class<E> type, Function<STDocument, E> constructor,
			Oid<?> oid);

}
