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

public final class AK_NET {
  /**
   * TODO.
   */
  public static final AttributeGroupKey NET = new AttributeGroupKey(com.sandpolis.core.profile.store.DomainStore.get("com.sandpolis.plugin.sysinfo"), 6, 0);

  /**
   * TODO.
   */
  public static final AttributeKey<String> FQDN = AttributeKey.newBuilder(NET, 1).setDotPath("net.fqdn").build();

  /**
   * TODO.
   */
  public static final AttributeKey<String> HOSTNAME = AttributeKey.newBuilder(NET, 2).setDotPath("net.hostname").build();
}
