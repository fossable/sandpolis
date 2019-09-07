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

public final class AK_DISPLAY {
  /**
   * TODO.
   */
  public static final AttributeGroupKey DISPLAY = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 2, 1);

  /**
   * The display's name.
   */
  public static final AttributeKey<String> NAME = AttributeKey.newBuilder(DISPLAY, 1).setDotPath("display.name").build();

  /**
   * The display's current resolution.
   */
  public static final AttributeKey<String> RESOLUTION = AttributeKey.newBuilder(DISPLAY, 2).setDotPath("display.resolution").build();

  /**
   * The display's physical size in pixels.
   */
  public static final AttributeKey<String> SIZE = AttributeKey.newBuilder(DISPLAY, 3).setDotPath("display.size").build();

  /**
   * Refresh frequency in Hertz.
   */
  public static final AttributeKey<Integer> REFRESH_FREQUENCY = AttributeKey.newBuilder(DISPLAY, 4).setDotPath("display.refresh_frequency").build();

  /**
   * TODO.
   */
  public static final AttributeKey<Integer> BIT_DEPTH = AttributeKey.newBuilder(DISPLAY, 5).setDotPath("display.bit_depth").build();
}
