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
module com.sandpolis.plugin.shell.agent.vanilla {
	exports com.sandpolis.plugin.shell.agent.vanilla.exe;
	exports com.sandpolis.plugin.shell.agent.vanilla.shell;
	exports com.sandpolis.plugin.shell.agent.vanilla.stream;
	exports com.sandpolis.plugin.shell.agent.vanilla;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.plugin.shell;
	requires org.slf4j;

	provides com.sandpolis.core.instance.plugin.SandpolisPlugin with com.sandpolis.plugin.shell.agent.vanilla.ShellPlugin;
}
