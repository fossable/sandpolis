package com.sandpolis.core.server.agentbuilder.deployer;

import com.sandpolis.core.server.agentbuilder.packager.PackagedAgent;

public interface AgentDeployer {

	public void run(PackagedAgent agent) throws Exception;
}
