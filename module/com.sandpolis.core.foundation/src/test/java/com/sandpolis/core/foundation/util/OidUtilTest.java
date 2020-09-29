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
