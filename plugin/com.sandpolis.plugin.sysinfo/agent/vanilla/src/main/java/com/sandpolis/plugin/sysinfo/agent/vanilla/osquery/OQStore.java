//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.plugin.sysinfo.agent.vanilla.osquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.store.StoreBase;

public class OQStore extends StoreBase {

	private static final Logger log = LoggerFactory.getLogger(OQStore.class);

	protected OQStore() {
		super(log);
	}

}
