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

public final class AK_LINUX {
  /**
   * TODO.
   */
  public static final AttributeGroupKey LINUX = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 8, 0);

  /**
   * TODO.
   */
  public static final AttributeKey<String> DISTRIBUTION = AttributeKey.newBuilder(LINUX, 1).setDotPath("linux.distribution").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> SHELL = AttributeKey.newBuilder(LINUX, 2).setDotPath("linux.shell").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> WINDOW_MANAGER = AttributeKey.newBuilder(LINUX, 3).setDotPath("linux.window_manager").build();
}
