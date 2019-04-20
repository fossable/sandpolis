/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.attribute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AttributeGroupTest {

	@Test
	@DisplayName("Check that groups report the correct anonimity")
	void isAnonymous() {
		assertTrue(new AttributeGroup().isAnonymous());

		AttributeDomainKey domain = new AttributeDomainKey("test");
		assertFalse(new AttributeGroup(new AttributeGroupKey(domain, 15, 0)).isAnonymous());
		assertFalse(new AttributeGroup(new AttributeGroupKey(domain, 16, 1)).isAnonymous());
		assertFalse(new AttributeGroup(new AttributeGroupKey(domain, 17, 2)).isAnonymous());
		assertFalse(new AttributeGroup(new AttributeGroupKey(domain, 18, 3)).isAnonymous());
		assertFalse(new AttributeGroup(new AttributeGroupKey(domain, 19, 4)).isAnonymous());
	}

}
