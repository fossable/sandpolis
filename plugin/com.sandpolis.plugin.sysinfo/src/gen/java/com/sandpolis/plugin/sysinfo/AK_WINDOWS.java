// This file is automatically generated. Do not edit!
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
  public static final AttributeKey<String> IE_VERSION = AttributeKey.newBuilder(WINDOWS, 1).build();

  /**
   * TODO.
   */
  public static final AttributeKey<Long> INSTALL_TIMESTAMP = AttributeKey.newBuilder(WINDOWS, 2).build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> POWERSHELL_VERSION = AttributeKey.newBuilder(WINDOWS, 3).build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> SERIAL = AttributeKey.newBuilder(WINDOWS, 4).build();
}