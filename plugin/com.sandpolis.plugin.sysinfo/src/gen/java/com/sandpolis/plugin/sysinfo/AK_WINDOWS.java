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

public final class AK_WINDOWS {
  /**
   * TODO.
   */
  public static final AttributeGroupKey WINDOWS = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 9, 0);

  /**
   * TODO.
   */
  public static final AttributeKey<String> IE_VERSION = AttributeKey.newBuilder(WINDOWS, 1).setDotPath("windows.ie_version").build();

  /**
   * TODO.
   */
  public static final AttributeKey<Long> INSTALL_TIMESTAMP = AttributeKey.newBuilder(WINDOWS, 2).setDotPath("windows.install_timestamp").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> POWERSHELL_VERSION = AttributeKey.newBuilder(WINDOWS, 3).setDotPath("windows.powershell_version").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> SERIAL = AttributeKey.newBuilder(WINDOWS, 4).setDotPath("windows.serial").build();
}
