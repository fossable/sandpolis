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

public final class AK_LOCATION {
  /**
   * TODO.
   */
  public static final AttributeGroupKey LOCATION = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 5, 0);

  /**
   * TODO.
   */
  public static final AttributeKey<String> CITY = AttributeKey.newBuilder(LOCATION, 1).setDotPath("location.city").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> COUNTRY = AttributeKey.newBuilder(LOCATION, 2).setDotPath("location.country").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> COUNTRY_CODE = AttributeKey.newBuilder(LOCATION, 3).setDotPath("location.country_code").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> LATITUDE = AttributeKey.newBuilder(LOCATION, 4).setDotPath("location.latitude").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> LONGITUDE = AttributeKey.newBuilder(LOCATION, 5).setDotPath("location.longitude").build();
}
