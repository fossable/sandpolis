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
 * This extension is used in module build scripts to configure the code
 * generator.
 */
public class ConfigExtension {

	/**
	 * The profile tree specification JSON file.
	 */
	public File profileTreeSpec;

	/**
	 * The type of profile tree to generate. Values are: javafx, core.
	 */
	public String profileTreeType;
}
