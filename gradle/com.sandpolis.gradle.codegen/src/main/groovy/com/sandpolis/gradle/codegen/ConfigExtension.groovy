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
package com.sandpolis.gradle.codegen

import java.io.File

/**
 * This extension can be used in subproject build scripts to configure the code
 * generator.
 */
class ConfigExtension {

	/**
	 * The attribute specification file.
	 */
	File attributeSpec

	/**
	 * The type of document bindings to generate. Values are: javafx, core.
	 */
	String documentBindings

	/**
	 * Whether to generate attribute implementations
	 */
	Boolean attributeImplementations
}
