//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
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
