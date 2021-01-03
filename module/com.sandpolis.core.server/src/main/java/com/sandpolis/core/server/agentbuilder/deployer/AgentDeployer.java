//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.agentbuilder.deployer;

import com.sandpolis.core.server.agentbuilder.packager.PackagedAgent;

public interface AgentDeployer {

	public void run(PackagedAgent agent) throws Exception;
}
