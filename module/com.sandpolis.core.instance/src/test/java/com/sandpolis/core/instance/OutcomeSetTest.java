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
package com.sandpolis.core.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sandpolis.core.proto.util.Result.Outcome;

public class OutcomeSetTest {

	@Test
	public void basicUsage() {
		OutcomeSet set = new OutcomeSet();
		set.add(Outcome.newBuilder().setResult(true).setComment("No comment").setTime(1023).setAction("Test1"));
		set.add(Outcome.newBuilder().setResult(true).setComment("Test2"));
		set.add(Outcome.newBuilder().setResult(true));

		assertTrue(set.getResult());
		assertNull(set.getOneFailed());
		assertTrue(set.getFailed().size() == 0);

		set.add(Outcome.newBuilder().setResult(false).setComment("Failed for some unknown reason :)"));

		assertFalse(set.getResult());
		assertNotNull(set.getOneFailed());
		assertTrue(set.getFailed().size() == 1);
		assertEquals("Failed for some unknown reason :)", set.getOneFailed().getComment());
	}

}
