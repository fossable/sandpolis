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
package com.sandpolis.core.attribute.key;

import com.sandpolis.core.attribute.AttributeGroupKey;
import com.sandpolis.core.attribute.AttributeKey;

public final class AK_INSTANCE {
  /**
   * Attributes applicable to all instances.
   */
  public static final AttributeGroupKey INSTANCE = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get(null), 1, 0);

  /**
   * Client/Viewer ID.
   */
  public static final AttributeKey<Integer> CVID = AttributeKey.newBuilder(INSTANCE, 1).setDotPath("instance.cvid").build();

  /**
   * Universally Unique ID.
   */
  public static final AttributeKey<String> UUID = AttributeKey.newBuilder(INSTANCE, 2).setDotPath("instance.uuid").build();

  /**
   * The instance's Sandpolis version string.
   */
  public static final AttributeKey<String> VERSION = AttributeKey.newBuilder(INSTANCE, 3).setDotPath("instance.version").build();

  /**
   * The instance's installation timestamp.
   */
  public static final AttributeKey<Long> INSTALL_TIMESTAMP = AttributeKey.newBuilder(INSTANCE, 4).setDotPath("instance.install_timestamp").build();
}
