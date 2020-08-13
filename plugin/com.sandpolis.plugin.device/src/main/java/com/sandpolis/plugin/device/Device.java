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
package com.sandpolis.plugin.device;

import com.sandpolis.core.instance.state.Document;

public class Device extends StateTree.Device {

	public Device(Document document) {
		super(document);
	}

	private String id;

	/**
	 * The UUID of the instance considered to be the device's "handler".
	 */
	private String handler;

	public String getId() {
		return "";
	}
}
