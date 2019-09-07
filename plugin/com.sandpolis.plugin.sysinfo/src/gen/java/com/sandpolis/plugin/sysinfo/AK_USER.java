/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.plugin.sysinfo;

import com.sandpolis.core.attribute.AttributeGroupKey;
import com.sandpolis.core.attribute.AttributeKey;

public final class AK_USER {
  /**
   * TODO.
   */
  public static final AttributeGroupKey USER = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 4, 0);

  /**
   * TODO.
   */
  public static final AttributeKey<String> ID = AttributeKey.newBuilder(USER, 1).setDotPath("user.id").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> USERNAME = AttributeKey.newBuilder(USER, 2).setDotPath("user.username").build();

  /**
   * The user's home directory.
   */
  public static final AttributeKey<String> HOME = AttributeKey.newBuilder(USER, 3).setDotPath("user.home").build();
}
