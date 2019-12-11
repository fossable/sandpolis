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
package com.sandpolis.server.vanilla.auth;

import com.sandpolis.server.vanilla.store.group.Group;

/**
 * An {@link AuthenticationMechanism} can be added to a {@link Group} which will
 * then allow clients to authenticate with that group.<br>
 * <br>
 *
 * Groups can have any number of {@code AuthenticationMechanism}s. A group with
 * no {@code AuthenticationMechanism}s will effectively become an insecure group
 * which allows any client to authenticate.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class AuthenticationMechanism {
}
