//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.foundation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OidUtilTest {

	@Test
	@DisplayName("Compute collection tags")
	void testComputeCollectionTag() {
		assertEquals(OidUtil.OTYPE_COLLECTION, OidUtil.getOidType(OidUtil.computeCollectionTag(12)));
	}
}
