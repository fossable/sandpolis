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
package com.sandpolis.plugin.device.snmp;

import com.sandpolis.core.instance.data.Document;
import com.sandpolis.plugin.device.Device;

public class SNMPv3Device extends Device {

	public SNMPv3Device(Document document) {
		super(document);
		// TODO Auto-generated constructor stub
	}
	private String securityLevel;
	private String securityName;
	private String authProtocol;
	private String authSecret;
	private String privProtocol;
	private String privSecret;

}
