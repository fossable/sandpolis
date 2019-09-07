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

public final class AK_OS {
  /**
   * TODO.
   */
  public static final AttributeGroupKey OS = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 7, 0);

  /**
   * TODO.
   */
  public static final AttributeKey<String> ARCH = AttributeKey.newBuilder(OS, 1).setDotPath("os.arch").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> WINDOW_TITLE = AttributeKey.newBuilder(OS, 2).setDotPath("os.window_title").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> FAMILY = AttributeKey.newBuilder(OS, 3).setDotPath("os.family").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> LANGUAGE = AttributeKey.newBuilder(OS, 4).setDotPath("os.language").build();

  /**
   * TODO.
   */
  public static final AttributeKey<Long> START_TIMESTAMP = AttributeKey.newBuilder(OS, 5).setDotPath("os.start_timestamp").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> TIMEZONE = AttributeKey.newBuilder(OS, 6).setDotPath("os.timezone").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> NAME = AttributeKey.newBuilder(OS, 7).setDotPath("os.name").build();
}
