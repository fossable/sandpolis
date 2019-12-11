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
package com.sandpolis.gradle.plugin

class ConfigExtension {

	/**
	 * The plugin's Sandpolis ID.
	 */
	String id

	/**
	 * The plugin's Maven group and artifact name.
	 */
	String coordinate

	/**
	 * The plugin's user-friendly name.
	 */
	String name

	/**
	 * The plugin's user-friendly description.
	 */
	String description
}
