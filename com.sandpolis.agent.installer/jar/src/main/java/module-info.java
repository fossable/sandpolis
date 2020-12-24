//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
module com.sandpolis.agent.installer.jar {
	exports com.sandpolis.agent.installer.jar;

	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.server;
	requires zipset;

	provides com.sandpolis.core.server.generator.Packager with com.sandpolis.agent.installer.JarPackager;
}
