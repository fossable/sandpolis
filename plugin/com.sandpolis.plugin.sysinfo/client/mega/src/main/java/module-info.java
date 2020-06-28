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
module com.sandpolis.plugin.sysinfo.client.mega {
	exports com.sandpolis.plugin.sysinfo.client.mega;

	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;

	provides com.sandpolis.core.instance.plugin.SandpolisPlugin with com.sandpolis.plugin.sysinfo.client.mega.SysinfoPlugin;
}
