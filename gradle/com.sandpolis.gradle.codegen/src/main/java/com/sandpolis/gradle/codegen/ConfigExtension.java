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
package com.sandpolis.gradle.codegen;

import java.io.File;

/**
 * This extension is used in build scripts to configure the code generator.
 */
public class ConfigExtension {

	/**
	 * The state tree specification JSON file for the instance.
	 */
	public File stateTree;

	/**
	 * Whether a JavaFX-specific state tree will be generated.
	 */
	public boolean javaFxStateTree = false;

	/**
	 * Whether the core state tree will be generated.
	 */
	public boolean coreStateTree = true;
}
