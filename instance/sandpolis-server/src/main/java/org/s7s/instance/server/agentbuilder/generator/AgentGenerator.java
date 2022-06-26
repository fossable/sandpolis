//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.agentbuilder.generator;

import org.s7s.core.server.group.Group;

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
