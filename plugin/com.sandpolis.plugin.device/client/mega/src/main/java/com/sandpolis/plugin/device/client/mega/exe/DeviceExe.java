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
package com.sandpolis.plugin.device.client.mega.exe;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.plugin.device.MessageDevice.DeviceMSG;
import com.sandpolis.plugin.device.MsgDevice.RQ_RegisterDevice;

public final class DeviceExe extends Exelet {

	@Auth
	@Handler(tag = DeviceMSG.RQ_REGISTER_DEVICE_FIELD_NUMBER)
	public static MessageOrBuilder rq_register_device(RQ_RegisterDevice rq) throws Exception {
		// TODO
		return null;
	}

	private DeviceExe() {
	}
}