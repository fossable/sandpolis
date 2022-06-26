//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.agentbuilder.deployer;

import org.s7s.core.server.agentbuilder.packager.PackagedAgent;

public interface AgentDeployer {

	public void run(PackagedAgent agent) throws Exception;
}
