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
package com.sandpolis.charcoal;

import static com.sandpolis.core.instance.Environment.printEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The entry point for Charcoal instances.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class Charcoal {
	private Charcoal() {
	}

	public static final Logger log = LoggerFactory.getLogger(Charcoal.class);

	public static void main(String[] args) {
		printEnvironment(log, "Sandpolis Charcoal");
	}
}
