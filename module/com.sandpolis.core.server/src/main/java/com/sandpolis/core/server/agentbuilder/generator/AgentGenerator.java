//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.agentbuilder.generator;

import com.sandpolis.core.server.group.Group;

/**
 * {@link AgentGenerator} is a base class for all artifact generators. There are
 * currently two generators: {@link VanillaGenerator} and
 * {@link MicroGenerator}.
 *
 * @since 4.0.0
 */
public interface AgentGenerator {

	/**
	 * Performs the generation synchronously.
	 */
	public GeneratedAgent run(Group group) throws Exception;
}
