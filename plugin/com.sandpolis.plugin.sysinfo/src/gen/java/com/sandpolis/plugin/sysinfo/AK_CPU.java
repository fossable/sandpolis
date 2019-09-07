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

public final class AK_CPU {
  /**
   * TODO.
   */
  public static final AttributeGroupKey CPU = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 1, 1);

  /**
   * TODO.
   */
  public static final AttributeKey<String> MODEL = AttributeKey.newBuilder(CPU, 1).setDotPath("cpu.model").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> VENDOR = AttributeKey.newBuilder(CPU, 2).setDotPath("cpu.vendor").build();

  /**
   * The specified frequency in Hertz.
   */
  public static final AttributeKey<Integer> FREQUENCY_SPEC = AttributeKey.newBuilder(CPU, 3).setDotPath("cpu.frequency_spec").build();

  /**
   * The size of the L1 cache in bytes.
   */
  public static final AttributeKey<Integer> L1_CACHE = AttributeKey.newBuilder(CPU, 4).setDotPath("cpu.l1_cache").setStatic(true).build();

  /**
   * The size of the L2 cache in bytes.
   */
  public static final AttributeKey<Integer> L2_CACHE = AttributeKey.newBuilder(CPU, 5).setDotPath("cpu.l2_cache").setStatic(true).build();

  /**
   * The size of the L3 cache in bytes.
   */
  public static final AttributeKey<Integer> L3_CACHE = AttributeKey.newBuilder(CPU, 6).setDotPath("cpu.l3_cache").setStatic(true).build();

  /**
   * The size of the L4 cache in bytes.
   */
  public static final AttributeKey<Integer> L4_CACHE = AttributeKey.newBuilder(CPU, 7).setDotPath("cpu.l4_cache").setStatic(true).build();

  /**
   * TODO.
   */
  public static final AttributeGroupKey core = new AttributeGroupKey(CPU, 8, 1);

  /**
   * The core's usage as a decimal.
   */
  public static final AttributeKey<Double> USAGE = AttributeKey.newBuilder(core, 1).setDotPath("core.usage").build();

  /**
   * The core's temperature in Celsius.
   */
  public static final AttributeKey<Double> TEMPERATURE = AttributeKey.newBuilder(core, 2).setDotPath("core.temperature").build();

  /**
   * TODO.
   */
  public static final AttributeGroupKey thread = new AttributeGroupKey(core, 3, 1);

  /**
   * The thread's ID.
   */
  public static final AttributeKey<Double> ID = AttributeKey.newBuilder(thread, 1).setDotPath("thread.id").build();
}
